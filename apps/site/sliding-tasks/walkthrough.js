document.querySelectorAll('[data-walkthrough]').forEach((container) => {
  const button = container.querySelector('button');
  button.addEventListener('click', () => {
    const frame = document.createElement('iframe');
    const parameters = new URLSearchParams({ hl: container.dataset.language, rel: '0' });
    frame.src = `https://www.youtube-nocookie.com/embed/${container.dataset.videoId}?${parameters}`;
    frame.title = button.dataset.playerTitle;
    frame.allow = 'encrypted-media; picture-in-picture; fullscreen';
    frame.allowFullscreen = true;
    frame.referrerPolicy = 'strict-origin-when-cross-origin';
    frame.tabIndex = 0;
    button.replaceWith(frame);
    frame.focus();
  });
});
