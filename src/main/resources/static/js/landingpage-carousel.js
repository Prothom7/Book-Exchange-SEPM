document.addEventListener("DOMContentLoaded", function () {
  const siteHeader = document.querySelector(".site-header");
  function syncHeaderHeight() {
    if (!siteHeader) {
      return;
    }
    document.documentElement.style.setProperty("--site-header-height", `${siteHeader.offsetHeight}px`);
  }

  syncHeaderHeight();
  window.addEventListener("resize", syncHeaderHeight);

  const carousel = document.querySelector(".hero-carousel");
  if (carousel) {
    const track = carousel.querySelector(".carousel-track");
    const slides = Array.from(carousel.querySelectorAll(".carousel-slide"));
    const dots = Array.from(carousel.querySelectorAll(".carousel-dots span"));

    if (track && slides.length > 1) {
      let currentIndex = 0;

      function renderSlide(index) {
        track.style.transform = `translateX(-${index * 100}%)`;
        dots.forEach((dot, dotIndex) => {
          dot.classList.toggle("active", dotIndex === index);
        });
      }

      renderSlide(currentIndex);

      setInterval(function () {
        currentIndex = (currentIndex + 1) % slides.length;
        renderSlide(currentIndex);
      }, 4500);
    }
  }

  const railButtons = Array.from(document.querySelectorAll(".feed-more"));
  railButtons.forEach(function (button) {
    button.addEventListener("click", function () {
      const targetId = button.getAttribute("data-target");
      if (!targetId) {
        return;
      }

      const rail = document.getElementById(targetId);
      if (!rail) {
        return;
      }

      const firstCard = rail.querySelector(".feed-card");
      const step = firstCard ? firstCard.clientWidth * 1.35 : Math.max(280, rail.clientWidth * 0.85);

      rail.scrollBy({
        left: step,
        behavior: "smooth"
      });
    });
  });
});
