/*
 * smithy.io/learn — shared quiz widget.
 * Zero dependencies, no network. Retrieval-practice self-check with instant feedback.
 *
 * Markup contract (authored per-lesson):
 *   <div class="quiz" data-answer="1">
 *     <p class="eyebrow">Check yourself</p>
 *     <p class="q">Question text?</p>
 *     <button class="quiz-opt">Option zero</button>
 *     <button class="quiz-opt">Option one</button>
 *     ...
 *     <div class="quiz-fb right">Shown when correct.</div>   (optional custom copy)
 *     <div class="quiz-fb nope">Shown when wrong.</div>       (optional custom copy)
 *   </div>
 *
 * data-answer is the zero-based index of the correct .quiz-opt.
 * Per the teach-skill rule, all options should be the same length so formatting leaks nothing.
 */
(function () {
  function wire(quiz) {
    var answer = parseInt(quiz.getAttribute("data-answer"), 10);
    var opts = Array.prototype.slice.call(quiz.querySelectorAll(".quiz-opt"));
    var fbRight = quiz.querySelector(".quiz-fb.right");
    var fbNope = quiz.querySelector(".quiz-fb.nope");
    var answered = false;

    opts.forEach(function (opt, i) {
      opt.addEventListener("click", function () {
        if (answered) return;
        answered = true;
        opts.forEach(function (o, j) {
          o.disabled = true;
          if (j === answer) o.classList.add("correct");
        });
        if (i !== answer) opt.classList.add("wrong");
        var fb = i === answer ? fbRight : fbNope;
        if (fb) fb.classList.add("show");
        // allow retry: clicking any option again re-opens
      });
    });

    // Optional "try again" affordance: double-click the question to reset.
    var q = quiz.querySelector(".q");
    if (q) {
      q.title = "Double-click to reset";
      q.addEventListener("dblclick", function () {
        answered = false;
        opts.forEach(function (o) { o.disabled = false; o.classList.remove("correct", "wrong"); });
        if (fbRight) fbRight.classList.remove("show");
        if (fbNope) fbNope.classList.remove("show");
      });
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    Array.prototype.slice.call(document.querySelectorAll(".quiz")).forEach(wire);
  });
})();
