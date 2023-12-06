package kr.craft.javaboilercraft.processor.impl.restdoc.core

import com.intellij.psi.*
import kr.craft.javaboilercraft.processor.util.EditorUtils.getIndent
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


/**
 * MockMvcTestBoilerplateGenerator
 *
 * @author jaypark
 * @version 1.0.0
 * @since 11/30/23
 */
object MockMvcTestBoilerplateGenerator {

    fun generateBoilerplate(methodProperty: MethodProperty, targetElement: PsiElement): String {

        val defaultIndent = getIndent(targetElement).getIndentString()

        val mockMvcTestBoilerplate = StringBuilder()
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(0)}@Test\n")
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(0)}void ${methodProperty.name}() throws Exception {\n")
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(1)}mockMvc.perform(\n")
        mockMvcTestBoilerplate.append(getHttpMethodAndEndPoint(methodProperty,defaultIndent))
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(3)}.contentType(MediaType.APPLICATION_JSON)\n")

        if (methodProperty.requestBody != null) {
            mockMvcTestBoilerplate.append(
                "${defaultIndent}${getIndentPrefix(3)}.accept(MediaType.APPLICATION_JSON)\n" +
                        "${defaultIndent}${getIndentPrefix(3)}.content(\n" +
                        "${defaultIndent}${getIndentPrefix(4)}objectMapper.writeValueAsString( requestBody )\n" +
                        "${defaultIndent}${getIndentPrefix(3)})\n"
            )
        }
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(1)})\n")
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(1)}.andDo(print())\n")
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(1)}.andDo(\n")
        mockMvcTestBoilerplate.append("${defaultIndent}${getIndentPrefix(2)}document(\"\"")
        if (methodProperty.pathVariables.isNotEmpty())
            mockMvcTestBoilerplate.append(
                getPathParametersDocs(methodProperty, defaultIndent)
            )

        if (methodProperty.queryParams.isNotEmpty())
            mockMvcTestBoilerplate.append(
                getQueryParamsDocs(methodProperty, defaultIndent)
            )

        if (methodProperty.requestBody != null) {
            mockMvcTestBoilerplate.append(
                getRequestFieldsDocs(methodProperty.requestBody, defaultIndent)
            )
        }

        if (methodProperty.responseType != null) {
            mockMvcTestBoilerplate.append(
                getResponseFieldsDocs(methodProperty.responseType, defaultIndent)
            )
        }

        mockMvcTestBoilerplate.append("\n${defaultIndent}${getIndentPrefix(2)})")
        mockMvcTestBoilerplate.append("\n${defaultIndent}${getIndentPrefix(1)});")
        mockMvcTestBoilerplate.append("\n${defaultIndent}}\n")

        return mockMvcTestBoilerplate.toString()
    }

    private fun getRequestFieldsDocs(requestBody: PsiParameter, defaultIndent: String): String {
        val requestBodyPsiClassType = requestBody.type as PsiClassType
        return ",\n" +
                "${defaultIndent}${getIndentPrefix(3)}requestFields(" +
                "\n${
                    generateRecursiveRestDocText(
                        requestBodyPsiClassType,
                        getCollectionPathPrefix(requestBodyPsiClassType),
                        AtomicInteger(10)
                    ).joinToString(
                        separator = ",\n"
                    ) { fieldWithPath ->
                        "${defaultIndent}${getIndentPrefix(4)}$fieldWithPath"
                    }
                }\n" +
                "${defaultIndent}${getIndentPrefix(3)})"
    }

    private fun getCollectionPathPrefix(psiClassType: PsiClassType): String {
        val psiClassInfo = psiClassType.resolve() ?: return ""
        if (TypeReference.isCollection(psiClassInfo.qualifiedName ?: ""))
            return "[]."
        return ""
    }

    private fun getResponseFieldsDocs(response: PsiType, defaultIndent: String): String {
        if (response.canonicalText == "void") return ""
        val responsePsiClassType = response as PsiClassType
        return ",\n" +
                "${defaultIndent}${getIndentPrefix(3)}responseFields(" +
                "\n${
                    generateRecursiveRestDocText(
                        responsePsiClassType,
                        getCollectionPathPrefix(responsePsiClassType),
                        AtomicInteger(10)
                    ).joinToString(
                        separator = ",\n"
                    ) { fieldWithPath ->
                        "${defaultIndent}${getIndentPrefix(4)}$fieldWithPath"
                    }
                }\n" +
                "${defaultIndent}${getIndentPrefix(3)})"
    }

    private fun getPsiClassFrom(
        targetClass: PsiClassType,
    ): PsiClassInfo? {
        val psiClass = targetClass.resolve() ?: return null
        if (TypeReference.isCollection(psiClass.qualifiedName ?: "")) {
            val collectionClass = (targetClass.parameters[0] as PsiClassType).resolve() ?: return null
            return PsiClassInfo(collectionClass, listOf(), "[].")
        }

        val typeParameterList =
            psiClass.typeParameters.map { typeParameter ->
                val psiType = targetClass.parameters[typeParameter.index]
                Pair(typeParameter, psiType)
            }.toList()
        return PsiClassInfo(psiClass, typeParameterList, "")
    }

    private fun getPathParametersDocs(methodProperty: MethodProperty, defaultIndent: String) =
        methodProperty.pathVariables.joinToString(
            prefix = ",\n" +
                    "${defaultIndent}${getIndentPrefix(3)}pathParameters(\n",
            separator = ",\n",
            postfix = "\n${defaultIndent}${getIndentPrefix(3)})"
        ) { pathVariable ->
            "${defaultIndent}${getIndentPrefix(5)}parameterWithName(\"${pathVariable.name}\").description(\"${pathVariable.name}\")"
        }

    private fun getQueryParamsDocs(methodProperty: MethodProperty, defaultIndent: String) =
        methodProperty.queryParams.joinToString(
            prefix = ",\n" +
                    "${defaultIndent}${getIndentPrefix(3)}queryParameters(\n",
            separator = ",\n",
            postfix = "\n${defaultIndent}${getIndentPrefix(3)})"
        ) { queryParam ->
            "${defaultIndent}${getIndentPrefix(5)}parameterWithName(\"${queryParam.name}\").description(\"${queryParam.name}\")"
        }

    private fun getHttpMethodAndEndPoint(methodProperty: MethodProperty, defaultIndent: String) =
        "${defaultIndent}${getIndentPrefix(2)}${methodProperty.httpMethodName}(${getRequestPath(methodProperty)})\n"

    private fun getRequestPath(methodProperty: MethodProperty): String {
        val baseEndPoint = methodProperty.requestPath
        var urlVariables = ""
        if (methodProperty.pathVariables.isNotEmpty()) {
            urlVariables = methodProperty.pathVariables.joinToString(
                prefix = urlVariables,
                separator = ", "
            ) { pathVariable -> pathVariable.name }
        }
        if (methodProperty.queryParams.isNotEmpty()) {
            val queryParams = methodProperty.queryParams.joinToString(
                prefix = "${baseEndPoint}?",
                separator = "&"
            ) { queryParam -> "${queryParam.name}={${queryParam.name}}" }

            urlVariables = methodProperty.queryParams.joinToString(
                prefix = if (urlVariables != "") "${urlVariables}, " else "",
                separator = ", "
            ) { queryParam -> queryParam.name }

            return "\"${queryParams}\", $urlVariables"
        }
        return if (urlVariables != "") "\"${baseEndPoint}\", ${urlVariables}" else "\"${baseEndPoint}\""
    }

    private fun generateRecursiveRestDocText(
        psiClassType: PsiClassType,
        beforeFieldName: String,
        atomicInteger: AtomicInteger,
    ): List<String> {

        val restDocList: MutableList<String> = ArrayList()
        if (atomicInteger.getAndAdd(-1) < 1) {
            return restDocList
        }

        val psiClassInfo = getPsiClassFrom(psiClassType) ?: return restDocList

        psiClassInfo.psiClass.fields.forEach { field: PsiField ->
            val type = getPsiType(psiClassInfo, field)

            val className = type.canonicalText
            var fieldName = beforeFieldName + field.name

            if (type is PsiPrimitiveType) {
                restDocList.add(generateFieldWithPathText(fieldName, field.name))
            }
            if (type is PsiClassType) {
                val resolvedClass = type.resolve()
                if (resolvedClass != null && isDocumentableClass(resolvedClass)) {
                    restDocList.add(generateFieldWithPathText(fieldName, field.name))
                } else if (TypeReference.isCollection(className)) {
                    fieldName += ".[]"
                    val parameter = type.parameters[0]
                    val genericClass = (parameter as PsiClassType)
                    val childClassType = getPsiTypeByClassName(
                        psiClassInfo,
                        genericClass.className
                    )

                    if(parameter is PsiPrimitiveType || isDocumentableClass(parameter.resolve()!!)){
                        restDocList.add(generateFieldWithPathText(fieldName, field.name))
                    }else if(childClassType != null){
                        fieldName += "."
                        restDocList.addAll(
                            generateRecursiveRestDocText(childClassType as PsiClassType, fieldName, atomicInteger)
                        )
                    }else{
                        fieldName += "."
                        restDocList.addAll(
                            generateRecursiveRestDocText(genericClass, fieldName, atomicInteger)
                        )
                    }

                } else {
                    if (resolvedClass == null) return@forEach
                    fieldName += "."
                    restDocList.addAll(
                        generateRecursiveRestDocText(
                            type,
                            fieldName,
                            atomicInteger
                        )
                    )
                }
            }
        }
        return restDocList
    }

    private fun getPsiType(
        psiClass: PsiClassInfo,
        type: PsiField,
    ) = psiClass.genericTypeParameters.find { typeParameter ->
        typeParameter.first.text == type.type.canonicalText
    }?.second ?: type.type

    private fun getPsiTypeByClassName(
        psiClass: PsiClassInfo,
        className: String,
    ) = psiClass.genericTypeParameters.find { typeParameter ->
        typeParameter.first.text == className
    }?.second

    private fun isDocumentableClass(targetClass: PsiClass): Boolean {
        return targetClass.isEnum || targetClass.qualifiedName?.let { qualifiedName ->
            return TypeReference.isDocumentableClass(qualifiedName)
        } ?: false
    }

    private fun getIndentPrefix(depth: Int) =
        "\t".repeat(depth)

    private fun generateFieldWithPathText(fieldNameWithPath: String, fieldName: String) =
        "fieldWithPath(\"$fieldNameWithPath\").description(\"$fieldName\")"
}

data class PsiClassInfo(
    val psiClass: PsiClass,
    val genericTypeParameters: List<Pair<PsiTypeParameter, PsiType>>,
    val pathPrefix: String,
)