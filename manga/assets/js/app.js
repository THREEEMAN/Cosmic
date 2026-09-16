/* ============================================================
   The Beginning of the End — reader engine
   No dependencies. Vanilla ES2017.
   ============================================================ */
(function () {
  "use strict";

  var PAGES  = window.MANGA_PAGES || [];
  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  var $  = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
  var clamp = function (v, a, b) { return v < a ? a : v > b ? b : v; };

  /* ---------------------------------------------------------
     1. Build the page sections from the manifest
     --------------------------------------------------------- */
  var reader = $("#reader");

  PAGES.forEach(function (p, i) {
    var sec = document.createElement("section");
    sec.className = "page-section";
    sec.id = p.id;
    sec.dataset.index = i;
    sec.setAttribute("aria-label", p.kicker + " — " + p.title);

    sec.innerHTML =
      '<div class="page-head">' +
        '<div class="page-num">' + p.num + "</div>" +
        '<div class="page-headings">' +
          '<p class="page-kicker">' + p.kicker +
            '<span class="lang-tag">' + p.lang + "</span></p>" +
          '<h2 class="page-title">' + p.title + "</h2>" +
          '<p class="page-places">' + p.places + "</p>" +
          '<p class="page-blurb">' + p.blurb + "</p>" +
        "</div>" +
      "</div>" +
      '<div class="page-stage">' +
        '<button class="page-frame" data-index="' + i + '" style="aspect-ratio:' + p.w + "/" + p.h + '"' +
          ' aria-label="Open ' + p.title + ' in the full-page reader">' +
          '<img class="ph" src="assets/thumbs/' + p.id + '.webp" alt="" aria-hidden="true" />' +
          '<img class="full" src="assets/pages/' + p.id + '.webp" alt="' + p.kicker + " — " + p.title +
            '" width="' + p.w + '" height="' + p.h + '" loading="lazy" decoding="async" />' +
          '<span class="page-expand">Read full page <b>⤢</b></span>' +
        "</button>" +
      "</div>" +
      '<p class="page-quote">' + p.quote + "</p>";

    reader.appendChild(sec);
  });

  $("#pageCount").textContent = PAGES.length;

  /* mark images loaded so the blur-up placeholder fades out */
  $$(".page-frame .full").forEach(function (img) {
    var frame = img.closest(".page-frame");
    if (img.complete && img.naturalWidth) frame.classList.add("loaded");
    else img.addEventListener("load", function () { frame.classList.add("loaded"); }, { once: true });
  });

  /* ---------------------------------------------------------
     2. Episode dots + index drawer
     --------------------------------------------------------- */
  var dotsWrap = $("#dots");
  PAGES.forEach(function (p, i) {
    var b = document.createElement("button");
    b.className = "dot";
    b.type = "button";
    b.dataset.index = i;
    b.title = p.kicker + " — " + p.title;
    b.setAttribute("aria-label", "Go to " + p.kicker);
    b.addEventListener("click", function () { goTo(i); });
    dotsWrap.appendChild(b);
  });
  var dots = $$(".dot", dotsWrap);

  var grid = $("#drawerGrid");
  PAGES.forEach(function (p, i) {
    var c = document.createElement("button");
    c.className = "idx-card";
    c.type = "button";
    c.innerHTML =
      '<img src="assets/thumbs/' + p.id + '.webp" alt="" loading="lazy" />' +
      "<span><span class=\"k\">" + p.kicker + "</span><span class=\"t\">" + p.title + "</span></span>";
    c.addEventListener("click", function () { closeDrawer(); goTo(i); });
    grid.appendChild(c);
  });

  var drawer = $("#drawer"), scrim = $("#scrim");
  function openDrawer() {
    drawer.classList.add("open");
    drawer.setAttribute("aria-hidden", "false");
    scrim.hidden = false;
    requestAnimationFrame(function () { scrim.classList.add("show"); });
    $("#btnIndex").setAttribute("aria-expanded", "true");
  }
  function closeDrawer() {
    drawer.classList.remove("open");
    drawer.setAttribute("aria-hidden", "true");
    scrim.classList.remove("show");
    setTimeout(function () { if (!drawer.classList.contains("open")) scrim.hidden = true; }, 400);
    $("#btnIndex").setAttribute("aria-expanded", "false");
  }
  $("#btnIndex").addEventListener("click", openDrawer);
  $("#btnIndex2").addEventListener("click", openDrawer);
  $("#drawerClose").addEventListener("click", closeDrawer);
  scrim.addEventListener("click", closeDrawer);

  function goTo(i) {
    var el = document.getElementById(PAGES[i].id);
    if (el) el.scrollIntoView({ behavior: reduce ? "auto" : "smooth", block: "start" });
  }
  $("#btnTop").addEventListener("click", function () {
    window.scrollTo({ top: 0, behavior: reduce ? "auto" : "smooth" });
  });

  /* ---------------------------------------------------------
     3. Scroll reveal
     --------------------------------------------------------- */
  var sections = $$(".page-section");

  var revealIO = new IntersectionObserver(function (entries) {
    entries.forEach(function (e) {
      if (e.isIntersecting) {
        e.target.classList.add("in");
        revealIO.unobserve(e.target);
      }
    });
  }, { rootMargin: "0px 0px -14% 0px", threshold: 0.12 });

  sections.forEach(function (s) { revealIO.observe(s); });

  $$(".reveal").forEach(function (el) {
    var d = parseInt(el.dataset.delay || "0", 10);
    var io = new IntersectionObserver(function (entries, obs) {
      entries.forEach(function (e) {
        if (!e.isIntersecting) return;
        setTimeout(function () { el.classList.add("in"); }, reduce ? 0 : d);
        obs.unobserve(el);
      });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.2 });
    io.observe(el);
  });

  /* ---------------------------------------------------------
     4. Scroll-linked effects: progress, parallax, active chapter
     --------------------------------------------------------- */
  var bar      = $("#progressBar");
  var topbar   = $("#topbar");
  var readout  = $("#chapterReadout");
  var heroBg   = $(".hero-bg");
  var frames   = $$(".page-frame");
  var ticking  = false;
  var active   = -1;

  function onScroll() {
    if (!ticking) { ticking = true; requestAnimationFrame(update); }
  }

  function update() {
    ticking = false;
    var y  = window.scrollY || window.pageYOffset;
    var vh = window.innerHeight;
    var max = document.documentElement.scrollHeight - vh;

    bar.style.width = (max > 0 ? clamp(y / max, 0, 1) * 100 : 0).toFixed(2) + "%";
    topbar.classList.toggle("solid", y > vh * 0.6);

    if (!reduce) {
      if (heroBg && y < vh * 1.3) {
        heroBg.style.transform = "scale(1.12) translate3d(0," + (y * 0.22).toFixed(1) + "px,0)";
      }
      /* gentle per-page parallax + focus once revealed */
      frames.forEach(function (f) {
        var r = f.getBoundingClientRect();
        if (r.bottom < -120 || r.top > vh + 120) return;
        var img = f.querySelector(".full");
        if (!img || !f.closest(".page-section").classList.contains("in")) return;
        /* -1 .. 1 across the viewport */
        var c = (r.top + r.height / 2 - vh / 2) / (vh / 2 + r.height / 2);
        img.style.transform = "scale(1.045) translate3d(0," + (c * -2.1).toFixed(2) + "%,0)";
      });
    }

    /* active chapter tracking */
    var idx = -1;
    for (var i = 0; i < sections.length; i++) {
      var t = sections[i].getBoundingClientRect().top;
      if (t <= vh * 0.42) idx = i; else break;
    }
    if (idx !== active) {
      active = idx;
      dots.forEach(function (d, i) { d.classList.toggle("active", i === idx); });
      readout.textContent = idx < 0 ? "Prologue" : PAGES[idx].kicker + " · " + PAGES[idx].title;
    }
  }

  window.addEventListener("scroll", onScroll, { passive: true });
  window.addEventListener("resize", onScroll);
  update();

  /* subtle pointer tilt on the page frames (desktop, fine pointer only) */
  if (!reduce && window.matchMedia("(hover:hover) and (pointer:fine)").matches) {
    frames.forEach(function (f) {
      f.addEventListener("pointermove", function (ev) {
        if (!f.closest(".page-section").classList.contains("in")) return;
        var r = f.getBoundingClientRect();
        var px = (ev.clientX - r.left) / r.width - 0.5;
        var py = (ev.clientY - r.top) / r.height - 0.5;
        f.style.transform = "perspective(1600px) rotateY(" + (px * 2.2).toFixed(2) +
                            "deg) rotateX(" + (-py * 2.2).toFixed(2) + "deg) translateZ(8px)";
      });
      f.addEventListener("pointerleave", function () { f.style.transform = ""; });
    });
  }

  /* ---------------------------------------------------------
     5. Lightbox: zoom, pan, keyboard, fullscreen
     --------------------------------------------------------- */
  var lb      = $("#lightbox");
  var lbImg   = $("#lbImg");
  var stage   = $("#lbStage");
  var lbTitle = $("#lbTitle");
  var lbSub   = $("#lbSub");
  var lbZoomL = $("#lbZoomLevel");
  var btnPrev = $("#lbPrev"), btnNext = $("#lbNext");

  var cur = 0, scale = 1, fitScale = 1, tx = 0, ty = 0, natW = 0, natH = 0;
  var MIN = 1, MAX = 6;

  function apply() {
    lbImg.style.transform = "translate3d(" + tx.toFixed(1) + "px," + ty.toFixed(1) + "px,0) scale(" + scale + ")";
    lbZoomL.textContent = Math.round((scale / fitScale) * 100) + "%";
  }

  function computeFit() {
    var pad = 28;
    var w = stage.clientWidth - pad * 2;
    var h = stage.clientHeight - pad * 2;
    if (!natW || !natH) return;
    fitScale = Math.min(w / natW, h / natH);
    MIN = fitScale;
    MAX = fitScale * 6;
    scale = fitScale;
    tx = (stage.clientWidth - natW * scale) / 2;
    ty = (stage.clientHeight - natH * scale) / 2;
    apply();
  }

  function constrain() {
    var w = natW * scale, h = natH * scale;
    var sw = stage.clientWidth, sh = stage.clientHeight;
    tx = w <= sw ? (sw - w) / 2 : clamp(tx, sw - w, 0);
    ty = h <= sh ? (sh - h) / 2 : clamp(ty, sh - h, 0);
  }

  function zoomAt(cx, cy, factor) {
    var next = clamp(scale * factor, MIN, MAX);
    if (next === scale) return;
    var k = next / scale;
    tx = cx - (cx - tx) * k;
    ty = cy - (cy - ty) * k;
    scale = next;
    constrain();
    apply();
  }

  function load(i, instant) {
    cur = clamp(i, 0, PAGES.length - 1);
    var p = PAGES[cur];
    lbTitle.textContent = p.title;
    lbSub.textContent = p.kicker + " · " + p.places;
    natW = p.w; natH = p.h;
    btnPrev.disabled = cur === 0;
    btnNext.disabled = cur === PAGES.length - 1;

    var src = "assets/pages/" + p.id + ".webp";
    if (instant) {
      lbImg.src = src; lbImg.alt = p.kicker + " — " + p.title;
      if (lbImg.complete && lbImg.naturalWidth) computeFit();
      else lbImg.addEventListener("load", computeFit, { once: true });
    } else {
      lbImg.classList.add("swapping");
      var pre = new Image();
      pre.onload = function () {
        lbImg.src = src; lbImg.alt = p.kicker + " — " + p.title;
        computeFit();
        lbImg.classList.remove("swapping");
      };
      pre.src = src;
    }
    /* preload neighbour */
    [cur - 1, cur + 1].forEach(function (n) {
      if (PAGES[n]) { var im = new Image(); im.src = "assets/pages/" + PAGES[n].id + ".webp"; }
    });
  }

  function open(i) {
    lb.classList.add("show");
    document.body.classList.add("locked");
    lb.setAttribute("aria-hidden", "false");
    requestAnimationFrame(function () { lb.classList.add("visible"); });
    load(i, true);
    $("#lbClose").focus();
  }
  function close() {
    lb.classList.remove("visible");
    lb.setAttribute("aria-hidden", "true");
    setTimeout(function () {
      lb.classList.remove("show");
      document.body.classList.remove("locked");
    }, 320);
    var f = frames[cur]; if (f) f.focus({ preventScroll: true });
  }

  frames.forEach(function (f) {
    f.addEventListener("click", function () { open(parseInt(f.dataset.index, 10)); });
  });

  $("#lbClose").addEventListener("click", close);
  btnPrev.addEventListener("click", function () { load(cur - 1); });
  btnNext.addEventListener("click", function () { load(cur + 1); });
  $("#lbZoomIn").addEventListener("click", function () { zoomAt(stage.clientWidth / 2, stage.clientHeight / 2, 1.35); });
  $("#lbZoomOut").addEventListener("click", function () { zoomAt(stage.clientWidth / 2, stage.clientHeight / 2, 1 / 1.35); });
  $("#lbFit").addEventListener("click", computeFit);
  $("#lbFull").addEventListener("click", function () {
    if (document.fullscreenElement) document.exitFullscreen();
    else if (lb.requestFullscreen) lb.requestFullscreen();
  });

  /* wheel zoom */
  stage.addEventListener("wheel", function (e) {
    e.preventDefault();
    var r = stage.getBoundingClientRect();
    zoomAt(e.clientX - r.left, e.clientY - r.top, e.deltaY < 0 ? 1.13 : 1 / 1.13);
  }, { passive: false });

  /* double click / tap toggles 1:1 */
  stage.addEventListener("dblclick", function (e) {
    var r = stage.getBoundingClientRect();
    if (scale > fitScale * 1.05) computeFit();
    else zoomAt(e.clientX - r.left, e.clientY - r.top, (fitScale * 2.6) / scale);
  });

  /* pointer pan + pinch */
  var pts = {}, last = null, pinch = null;
  stage.addEventListener("pointerdown", function (e) {
    stage.setPointerCapture(e.pointerId);
    pts[e.pointerId] = { x: e.clientX, y: e.clientY };
    var ids = Object.keys(pts);
    if (ids.length === 1) { last = { x: e.clientX, y: e.clientY }; stage.classList.add("grabbing"); }
    else if (ids.length === 2) {
      var a = pts[ids[0]], b = pts[ids[1]];
      pinch = { d: Math.hypot(a.x - b.x, a.y - b.y) };
    }
  });
  stage.addEventListener("pointermove", function (e) {
    if (!pts[e.pointerId]) return;
    pts[e.pointerId] = { x: e.clientX, y: e.clientY };
    var ids = Object.keys(pts);
    if (ids.length === 2 && pinch) {
      var a = pts[ids[0]], b = pts[ids[1]];
      var d = Math.hypot(a.x - b.x, a.y - b.y);
      var r = stage.getBoundingClientRect();
      zoomAt((a.x + b.x) / 2 - r.left, (a.y + b.y) / 2 - r.top, d / pinch.d);
      pinch.d = d;
      return;
    }
    if (!last) return;
    tx += e.clientX - last.x;
    ty += e.clientY - last.y;
    last = { x: e.clientX, y: e.clientY };
    constrain();
    apply();
  });
  ["pointerup", "pointercancel", "pointerleave"].forEach(function (ev) {
    stage.addEventListener(ev, function (e) {
      delete pts[e.pointerId];
      if (!Object.keys(pts).length) { last = null; pinch = null; stage.classList.remove("grabbing"); }
    });
  });

  window.addEventListener("resize", function () { if (lb.classList.contains("show")) computeFit(); });

  /* ---------------------------------------------------------
     6. Keyboard
     --------------------------------------------------------- */
  document.addEventListener("keydown", function (e) {
    if (lb.classList.contains("show")) {
      if (e.key === "Escape")      { e.preventDefault(); document.fullscreenElement ? document.exitFullscreen() : close(); }
      else if (e.key === "ArrowRight" || e.key === "PageDown") { e.preventDefault(); load(cur + 1); }
      else if (e.key === "ArrowLeft"  || e.key === "PageUp")   { e.preventDefault(); load(cur - 1); }
      else if (e.key === "+" || e.key === "=") { zoomAt(stage.clientWidth / 2, stage.clientHeight / 2, 1.35); }
      else if (e.key === "-" || e.key === "_") { zoomAt(stage.clientWidth / 2, stage.clientHeight / 2, 1 / 1.35); }
      else if (e.key === "0")      { computeFit(); }
      else if (e.key === "f" || e.key === "F") { $("#lbFull").click(); }
      return;
    }
    if (e.key === "Escape" && drawer.classList.contains("open")) closeDrawer();
    if (e.key === "i" || e.key === "I") { drawer.classList.contains("open") ? closeDrawer() : openDrawer(); }
    if (e.key === "ArrowDown" && e.shiftKey) { e.preventDefault(); goTo(clamp(active + 1, 0, PAGES.length - 1)); }
    if (e.key === "ArrowUp"   && e.shiftKey) { e.preventDefault(); goTo(clamp(active - 1, 0, PAGES.length - 1)); }
  });

  /* deep link: #ep04 opens that section, #ep04! opens the reader */
  if (location.hash) {
    var h = location.hash.slice(1).replace(/!$/, "");
    var k = PAGES.findIndex(function (p) { return p.id === h; });
    if (k >= 0 && /!$/.test(location.hash)) setTimeout(function () { open(k); }, 500);
  }
})();
