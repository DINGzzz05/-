document.querySelectorAll("[data-account]").forEach((button) => {
    button.addEventListener("click", () => {
        const form = button.closest("form");
        form.elements.status.value = button.dataset.role;
        form.elements.num.value = button.dataset.account;
        form.elements.password.value = "123456";
        form.querySelector("[type=submit]").focus();
    });
});
