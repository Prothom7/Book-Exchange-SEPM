document.addEventListener("DOMContentLoaded", function () {
  const carousel = document.querySelector(".hero-carousel");
  if (!carousel) {
    return;
  }

  const track = carousel.querySelector(".carousel-track");
  const slides = Array.from(carousel.querySelectorAll(".carousel-slide"));
  const dots = Array.from(carousel.querySelectorAll(".carousel-dots span"));

  if (!track || slides.length <= 1) {
    return;
  }

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
});
