var SakaiDrive = function() {
  this.setupPreviewer();
  this.setupRow();
};

SakaiDrive.prototype.setupPreviewer = function() {
  var self = this;

  $('.drive-file.pdf a, .drive-file.image a, .drive-file.excel a, .drive-file.ppt a, .drive-file.word a').on('click', function(event) {
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


SakaiDrive.prototype.setupRow = function() {
  var self = this;

  $('.sakai-resources-table').on('mousedown', 'tbody tr', function(event) {
    var $tr = $(this).closest('tr');

    if (!$tr.is('.active')) {
      $('.sakai-resources-table .active').removeClass('active');
      $tr.addClass('active');
    }

    return true;
 }).on('click', 'tbody tr', function(event) {
      var $tr = $(this).closest('tr');

      if ($(event.target).is('a')) {
        return true;
      }

      return false;
  }).on('dblclick', 'tbody tr', function(event) {
      var $tr = $(this).closest('tr');

      if ($(event.target).is('a')) {
        return true;
      }

      $tr.find('td.name a')[0].click();

      return false;
  });
};


SakaiDrive.prototype.VIEW_URL = '/drive-tool/pdfjs/web/viewer.html?file=';
