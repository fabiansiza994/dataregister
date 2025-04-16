 document.addEventListener("DOMContentLoaded", () => {
    const baseURL = "https://dataregisterapp.com/";

    // Corrige acciones de formularios sin / o http
    document.querySelectorAll("form").forEach(form => {
    const action = form.getAttribute("action");
    if (action && !action.startsWith("http") && !action.startsWith("/")) {
    form.setAttribute("action", baseURL + action);
}
});

    // Corrige llamadas fetch relativas sin barra
    const originalFetch = window.fetch;
    window.fetch = function(input, init) {
    if (typeof input === "string" && !input.startsWith("http") && !input.startsWith("/")) {
    input = baseURL + input;
}
    return originalFetch(input, init);
};
});