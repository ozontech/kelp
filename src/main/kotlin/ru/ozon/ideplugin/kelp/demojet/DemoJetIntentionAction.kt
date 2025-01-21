package ru.ozon.ideplugin.kelp.demojet

import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiBasedModCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.apache.oro.text.perl.Perl5Util
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import ru.ozon.ideplugin.kelp.KelpBundle
import ru.ozon.ideplugin.kelp.pluginConfig.DemoJetParameterToPropertyFunctionMapping
import ru.ozon.ideplugin.kelp.pluginConfig.kelpConfig

private data class FunctionParam(
    val name: Name,
    val type: String,
    val nullable: Boolean,
    val defaultValue: KtExpression?,
    val isDefaultValueNullable: Boolean?,
    val kdoc: String?,
)

class DemoJetIntentionAction : PsiBasedModCommandAction<KtCallExpression>(KtCallExpression::class.java) {
    override fun getFamilyName(): String = KelpBundle.message("demoJetIntentionName")

    override fun perform(context: ActionContext, element: KtCallExpression): ModCommand {
        val config = context.project.kelpConfig()?.demoJetDemosGeneration
        val nullablePropertyFunctionName = config?.nullablePropertyFunctionName
            ?.ifBlank { null }
            ?.let { Name.identifier(it) }
            ?: return ModCommand.nop()
        val mappings = config
            .parameterToPropertyFunctionMappings
            .takeIf { it.isNotEmpty() }
            ?: return ModCommand.nop()

        val functionOverloads: List<KtNamedFunction> = element.findFunctionOverloads().ifEmpty {
            return ModCommand.error(KelpBundle.message("demoJetIntentionNoFunctionsFound"))
        }

        val ktPsiFactory = KtPsiFactory(context.project)
        return ModCommand.chooseAction(
            KelpBundle.message("demoJetIntentionChooseOverloadDialogTitle"),
            functionOverloads.filterNot { it.valueParameterList == null }.map { function ->
                ModCommand.psiUpdateStep(
                    /* element = */ element,
                    /* title = */ function.valueParameters.joinToString { it.name.orEmpty() },
                    /* action = */ { element, updater ->
                        val functionParamsAndMappings =
                            matchParametersAndPropertyMappings(function, mappings, ktPsiFactory)

                        val valueArgumentList = ktPsiFactory.buildValueArgumentList {
                            appendFixedText("(")
                            functionParamsAndMappings.forEach { (functionParam, mapping) ->
                                appendValueParameter(functionParam, mapping, ktPsiFactory, nullablePropertyFunctionName)
                                appendFixedText(",\n")
                            }
                            appendFixedText(")")
                        }

                        element.valueArgumentList?.let(updater::getWritable)?.replace(valueArgumentList)
                    },
                    /* range = */ {
                        TextRange.EMPTY_RANGE
                    }
                )
            }
        )
    }

    override fun getPresentation(context: ActionContext, element: KtCallExpression): Presentation {
        return Presentation.of(KelpBundle.message("demoJetIntentionName"))
    }

    override fun stopSearchAt(element: PsiElement, context: ActionContext): Boolean {
        val config = context.project.kelpConfig()?.demoJetDemosGeneration
            ?.takeIf { it.parameterToPropertyFunctionMappings.isNotEmpty() }
            ?: return true
        val packageName = config.enableOnlyIn?.packageName


        val isWrongPackage = !packageName.isNullOrBlank() &&
                (context.file as? KtFile)?.packageDirective?.qualifiedName?.startsWith(packageName) != true
        if (isWrongPackage) return true

        return false
    }

    private fun KtCallExpression.findFunctionOverloads(): List<KtNamedFunction> = analyze(this) {
        resolveToCallCandidates().mapNotNull {
            ((it.candidate as? KaSimpleFunctionCall)?.symbol as? KaNamedFunctionSymbol)?.psi as? KtNamedFunction
        }
    }

    private fun BuilderByPattern<KtValueArgumentList>.appendValueParameter(
        functionParam: FunctionParam,
        mapping: DemoJetParameterToPropertyFunctionMapping?,
        ktPsiFactory: KtPsiFactory,
        nullablePropertyFunctionName: Name,
    ) {
        appendName(functionParam.name)
        appendFixedText(" = ")
        val propertyFunction = mapping?.propertyFunction
        if (mapping == null || propertyFunction == null) {
            appendFixedText("TODO()")
            return
        }
        if (functionParam.nullable) {
            appendName(nullablePropertyFunctionName)
            if (propertyFunction.includeDefault &&
                functionParam.defaultValue != null &&
                functionParam.isDefaultValueNullable == false
            ) {
                appendFixedText("(initiallyNull = false)")
            }
            appendFixedText(" { ")
        }

        appendName(Name.identifier(mapping.propertyFunction.name))
        appendFixedText("(")

        appendFixedText("""name = "${functionParam.name}", """)
        if (propertyFunction.includeDefault && functionParam.defaultValue != null &&
            (!functionParam.nullable || functionParam.isDefaultValueNullable == false)
        ) {
            appendFixedText("default = ")
            appendExpression(functionParam.defaultValue)
            appendFixedText(", ")
        }
        if (functionParam.kdoc != null) {
            val kdoc = functionParam.kdoc.replace("$", "\\$").replace("\"", "\\\"")
            val descriptionParamExpression = ktPsiFactory.createExpressionIfPossible("\"$kdoc\"")
            if (descriptionParamExpression != null) {
                appendFixedText("description = ")
                appendExpression(descriptionParamExpression)
            }
        }

        appendFixedText(")")

        if (functionParam.nullable) appendFixedText(" }")
    }

    private fun matchParametersAndPropertyMappings(
        function: KtNamedFunction,
        mappings: List<DemoJetParameterToPropertyFunctionMapping>,
        ktPsiFactory: KtPsiFactory,
    ): List<Pair<FunctionParam, DemoJetParameterToPropertyFunctionMapping?>> {
        val mappingsForUnwantedParameters = mappings.filter { it.propertyFunction == null }
        val paramNameToKDoc: Map<String, String>? = function.paramNamesToKDoc()

        return function.valueParameters.mapNotNull {
            val name = it.nameAsName ?: return@mapNotNull null
            analyze(it) {
                val returnType = it.symbol.returnType
                val defaultValue = it.defaultValue
                val functionParam = FunctionParam(
                    name = name,
                    type = (returnType as? KaClassType)?.classId?.asFqNameString() ?: return@analyze null,
                    defaultValue = defaultValue,
                    isDefaultValueNullable = defaultValue?.expressionType?.nullability?.isNullable,
                    kdoc = paramNameToKDoc?.getOrElse(name.asString()) { null },
                    nullable = returnType.isMarkedNullable,
                )

                if (mappingsForUnwantedParameters.findMappingFor(functionParam, returnType, this) != null) {
                    return@analyze null
                }

                val mapping = mappings.findMappingFor(functionParam, parameterType = returnType, this)

                val propertyFunction = mapping?.propertyFunction
                val functionParamWithReplacedDefaultValue =
                    processDefaultValueIfNeeded(functionParam, propertyFunction, ktPsiFactory)
                functionParamWithReplacedDefaultValue to mapping
            }
        }
    }

    /**
     * Applies [defaultRegexToReplace] regex to the default value of the function parameter and performs a replacement
     */
    private fun processDefaultValueIfNeeded(
        functionParam: FunctionParam,
        propertyFunction: DemoJetParameterToPropertyFunctionMapping.PropertyFunction?,
        ktPsiFactory: KtPsiFactory
    ) = if (functionParam.defaultValue != null && propertyFunction?.defaultRegexToReplace != null) {
        val pattern = propertyFunction.defaultRegexToReplace
        val input = functionParam.defaultValue.text
        val replacement = propertyFunction.defaultReplacement.orEmpty()
        val newDefaultValue = runCatching { Perl5Util().substitute("s/$pattern/$replacement/", input) }.getOrNull()
        val newDefaultValueExpression = newDefaultValue?.let(ktPsiFactory::createExpressionIfPossible)
        newDefaultValueExpression?.let { functionParam.copy(defaultValue = it) } ?: functionParam
    } else {
        functionParam
    }

    /**
     * Acquires parameter descriptions from KDoc of the function and maps them to the parameter names
     *
     * @return map of `param names` to `KDoc descriptions`, or null, if there is no KDoc comment
     */
    private fun KtNamedFunction.paramNamesToKDoc(): Map<String, String>? =
        docComment?.text
            ?.let(::parseDocString)
            ?.split("@param ")
            ?.drop(1)
            ?.map { it.replace('\n', ' ').trim() }
            ?.associateBy(
                keySelector = { it.takeWhile { !it.isWhitespace() } },
                valueTransform = { it.dropWhile { !it.isWhitespace() }.trimStart() }
            )

    /**
     * Finds a mapping ([DemoJetParameterToPropertyFunctionMapping]) from config for a function [parameter].
     */
    private fun List<DemoJetParameterToPropertyFunctionMapping>.findMappingFor(
        parameter: FunctionParam,
        parameterType: KaClassType,
        session: KaSession
    ): DemoJetParameterToPropertyFunctionMapping? = firstOrNull { mapping ->
        with(session) {
            val criteria = mapping.functionParameterCriteria

            val nameMatch = criteria.nameRegex?.matches(parameter.name.asString()) != false
            if (!nameMatch) return@firstOrNull false
            val typeMatch = criteria.typeRegex?.matches(parameter.type) != false
            if (!typeMatch) return@firstOrNull false
            val enumMatch = if (criteria.typeIsEnum != null) {
                val isEnum = (parameterType.symbol as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS
                isEnum == criteria.typeIsEnum
            } else {
                true
            }
            if (!enumMatch) return@firstOrNull false
            val subClassMatch = if (!criteria.typeIsSubclassOfAll.isNullOrEmpty()) {
                criteria.typeIsSubclassOfAll.all { superClassName ->
                    val superType = buildClassType(ClassId.fromString(superClassName.replace('.', '/')))
                    parameterType.isSubtypeOf(superType)
                }
            } else {
                true
            }
            if (!subClassMatch) return@firstOrNull false
            true
        }
    }


    /**
     * Removes stars and slashes from the KDoc string
     */
    private fun parseDocString(raw: String): String? {
        val t1 = raw.trim()
        if (!t1.startsWith("/**") || !t1.endsWith("*/"))
            return null
        val lineSep = t1.findAnyOf(listOf("\r\n", "\n", "\r"))?.second ?: ""
        return t1.trim('/', '*').lines().joinToString(lineSep) {
            it.trimStart().trimStart('*')
        }
    }
}