/**
 * 
 */
document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll(".custom-select-wrapper").forEach(wrapper => {
    const combo = wrapper.querySelector(".custom-select");
    const options = wrapper.querySelector(".custom-options");
	//const hidden = document.getElementById("Form:hiddenSelect");
	const hidden = wrapper.querySelector("input[type='hidden']");

    // Тоггле приказ опција
    combo.addEventListener("click", () => {
      options.style.display = options.style.display === "block" ? "none" : "block";
    });

    // Избор опције
    options.querySelectorAll(".custom-option").forEach(opt => {
      opt.addEventListener("click", () => {
        combo.textContent = opt.textContent;
        hidden.value = opt.dataset.value;
        options.style.display = "none";

        // За проверу у конзоли:
        console.log("Изабрано:", hidden.value);
		
		// Ако wrapper има data-submit, пошаљи форму
		  if (wrapper.dataset.submit === "true") {
		    wrapper.closest("form").submit();
		  }
      });
    });

    // Клик ван селекта затвара опције
    document.addEventListener("click", (e) => {
      if (!wrapper.contains(e.target)) {
        options.style.display = "none";
      }
    });
  });
});

