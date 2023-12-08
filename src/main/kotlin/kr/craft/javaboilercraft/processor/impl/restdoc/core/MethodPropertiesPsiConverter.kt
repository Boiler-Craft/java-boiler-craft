package kr.craft.javaboilercraft.processor.impl.restdoc.core

import com.intellij.psi.*
import org.apache.commons.lang.StringUtils.lowerCase

/**
 * MethodPropertiesPsiConverter
 *
 * @author jaypark
 * @version 1.0.0
 * @since 11/30/23
 */
object MethodPropertiesPsiConverter {

    private const val PATH_VARIABLE = "PathVariable"
    private const val REQUEST_PARAM = "RequestParam"
    private const val REQUEST_BODY = "RequestBody"

    @JvmStatic
    fun convert(targetClass: PsiClass, targetMethod: PsiMethod): MethodProperty? {
        val (httpMethodAnnotation, httpMethodMapping) = getHttpMethodAnnotation(targetMethod)
        if (httpMethodAnnotation == null || httpMethodMapping == null) return null
        val httpMethod = handleRequestMapping(httpMethodMapping, httpMethodAnnotation) ?: return null
        val requestPath = getRequestPath(targetClass , httpMethodAnnotation)
        val pathVariables = getPathVariableSize(targetMethod)
        val queryParams = getQueryParams(targetMethod)
        val requestBody = getRequestBody(targetMethod)
        val responseType = getResponseType(targetMethod)

        return MethodProperty(
            name = targetMethod.name,
            requestPath = requestPath,
            httpMethodName = httpMethod.name.lowercase(),
            pathVariables = pathVariables,
            queryParams = queryParams,
            requestBody = requestBody,
            responseType = responseType,
        )
    }

    @JvmStatic
    private fun getResponseType(targetMethod: PsiMethod) = targetMethod.returnType

    @JvmStatic
    private fun getRequestBody(targetMethod: PsiMethod) =
        targetMethod.parameterList.parameters.find { parameter ->
            parameter.annotations.any { annotation ->
                annotation.qualifiedName?.contains(REQUEST_BODY) == true
            }
        }
    @JvmStatic
    private fun getQueryParams(targetMethod: PsiMethod): List<PsiParameter> {
        return targetMethod.parameterList.parameters.filter { parameter ->
            parameter.annotations.any { annotation ->
                annotation.qualifiedName?.contains(REQUEST_PARAM) == true
            }
        }
    }

    @JvmStatic
    private fun getPathVariableSize(targetMethod: PsiMethod): List<PsiParameter> {
        return targetMethod.parameterList.parameters.filter { parameter ->
            parameter.annotations.any { annotation ->
                annotation.qualifiedName?.contains(PATH_VARIABLE) == true
            }
        }
    }

    @JvmStatic
    private fun getHttpMethodAnnotation(targetMethod: PsiMethod): Pair<PsiAnnotation?, HttpMethodMapping?> {
        var httpMethodMapping: HttpMethodMapping? = null
        val annotation = targetMethod.annotations.find { annotation ->
            httpMethodMapping = HttpMethodMapping.values().find { mapping ->
                annotation.qualifiedName?.contains(mapping.methodMapping) == true
            }
            httpMethodMapping != null
        }
        return Pair(annotation, httpMethodMapping)
    }

    @JvmStatic
    private fun getRequestPath(
        targetClass: PsiClass,
        httpMethodAnnotation: PsiAnnotation,
    ): String {
        val classLevelRequestMapping = targetClass.annotations.find { annotation ->
            annotation.qualifiedName?.contains(HttpMethodMapping.REQUEST_MAPPING.methodMapping) == true
        }.let { getPathMappingValue(it!!) }
        val methodLevelRequestMapping = getPathMappingValue(httpMethodAnnotation)

        return listOf(
            classLevelRequestMapping ,
            methodLevelRequestMapping
        ).joinToString( separator = "")

    }

    @JvmStatic
    private fun getPathMappingValue(httpMethodAnnotation: PsiAnnotation): String {
        val value = getAttributeValue(httpMethodAnnotation, "value")
        val path = getAttributeValue(httpMethodAnnotation, "path")
        return (if (value.length > path.length) value else path).replace("\"", "")
    }

    @JvmStatic
    private fun getAttributeValue(httpMethodAnnotation: PsiAnnotation, attributeName: String): String {
        return httpMethodAnnotation.findAttributeValue(attributeName)?.text?.replace("{}", "") ?: ""
    }

    @JvmStatic
    private fun handleRequestMapping(
        httpMethodMapping: HttpMethodMapping,
        httpMethodAnnotation: PsiAnnotation,
    ): HttpMethodMapping? {
        return if (httpMethodMapping != HttpMethodMapping.REQUEST_MAPPING)
            httpMethodMapping
        else {
            HttpMethodMapping.values().find { httpMethod ->
                (httpMethodAnnotation.findAttributeValue("method")?.text
                    ?.contains(httpMethod.name) == true)
            }
        }
    }
}

