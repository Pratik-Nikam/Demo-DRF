@ControllerAdvice
static class HealthzResponseShapeAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType,
                          Class<? extends HttpMessageConverter<?>> converterType) {
    return true; // filter by path in beforeBodyWrite
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object beforeBodyWrite(Object body,
                                MethodParameter returnType,
                                MediaType selectedContentType,
                                Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                ServerHttpRequest request,
                                ServerHttpResponse response) {

    // Only works for MVC (Servlet)
    if (!(request instanceof ServletServerHttpRequest servletReq)) return body;

    HttpServletRequest http = servletReq.getServletRequest();
    String uri = http.getRequestURI();
    if (uri == null) return body;

    // Adjust if your endpoint is exactly /healthz or /health-z
    if (!(uri.endsWith("/healthz") || uri.endsWith("/health-z"))) return body;

    // Only rewrite Map-shaped JSON
    if (!(body instanceof Map<?, ?> raw)) return body;

    Map<String, Object> root = new LinkedHashMap<>();
    raw.forEach((k, v) -> root.put(String.valueOf(k), v));

    Object detailsObj = root.get("details");
    if (!(detailsObj instanceof Map<?, ?> details)) return body;

    Object compsObj = ((Map<String, Object>) details).get("components");
    if (compsObj == null) return body;

    // Move details.components -> top-level components
    root.put("components", compsObj);
    root.remove("details");

    return root;
  }
}
