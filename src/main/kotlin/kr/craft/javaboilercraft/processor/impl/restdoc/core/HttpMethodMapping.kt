package kr.craft.javaboilercraft.processor.impl.restdoc.core

/**
 * HttpMethodMapping
 *
 * @author jaypark
 * @version 1.0.0
 * @since 12/1/23
 */
enum class HttpMethodMapping(val methodMapping: String) {
    CONNECT("ConnectMapping"),
    DELETE("DeleteMapping"),
    GET("GetMapping"),
    HEAD("HeadMapping"),
    OPTIONS("OptionsMapping"),
    PATCH("PatchMapping"),
    POST("PostMapping"),
    PUT("PutMapping"),
    TRACE("TraceMapping"),
    REQUEST_MAPPING("RequestMapping");

}