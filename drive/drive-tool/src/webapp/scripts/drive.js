var SakaiDrive = function() {
  this.setupPreviewer();
};

SakaiDrive.prototype.setupPreviewer = function() {
  var self = this;

  $('.drive-file.pdf a, .drive-file.excel a, .drive-file.ppt a, .drive-file.word a').on('click', function(event) {
    event.preventDefault();
    event.stopPropagation();

    var $preview = self.getPreviewTemplate();

    $preview.find('iframe').attr('src', self.VIEW_URL + encodeURIComponent(baseURL + 'preview?path=' + $(this).data('path')));

    $(document.body).append($preview);

    function resize() {
      $preview.height($(window).height());
      $preview.find('.blockout, iframe').height($preview.height());
    };

    $(window).on('resize', resize);
    resize();

    $(document.body).addClass('sakai-drive-preview-open');

    $preview.on('click', '.close', function() {
      $(document.body).removeClass('sakai-drive-preview-open');
      $preview.remove();
      $(window).off('resize', resize);
    });

    return false;
  });
};

SakaiDrive.prototype.getPreviewTemplate = function() {
  return $($("#previewTemplate").html().trim());
}

SakaiDrive.prototype.VIEW_URL = '/drive-tool/pdfjs/web/viewer.html?file=';
