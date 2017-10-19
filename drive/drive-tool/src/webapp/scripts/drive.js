var SakaiDrive = function() {
  this.setupPreviewer();
  this.setupRow();
  this.setupDragAndDrop();
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


SakaiDrive.prototype.setupDragAndDrop = function() {
    function doMove(source, target) {
      var $form = $('form#move-form');
      var $sourceInput = $form.find('[name="source"]');
      $sourceInput.val($(source).find('[data-path]').data('path'));
      var $targetInput = $form.find('[name="target"]');
      $targetInput.val($(target).find('[data-path]').data('path'));
      $form.submit();
    };

    // setup droppables
    var dragging;

    $('.sakai-resources .sakai-resources-breadcrumbs .breadcrumb-item:not(.active)').addClass('sakai-resource-dropzone');
    $('.sakai-resources .sakai-resources-table .drive-folder').each(function() {
        $(this).closest('tr').addClass('sakai-resource-dropzone');
    });


    $('.sakai-resources-table tr[draggable]').on('dragstart', function() {
        dragging = event.target;

        event.target.style.opacity = .6;
        event.dataTransfer.dragEffect = 'none';
        event.dataTransfer.dropEffect = 'move';
        event.dataTransfer.effectAllowed = 'move';
    }).on('dragend', function( event ) {
        event.target.style.opacity = "";
    });

    $('.sakai-resource-dropzone').on('dragover', function(event) {
        event.preventDefault();

        if (!$(event.target).closest('.sakai-resource-dropzone').is(dragging)) {
            $(event.target).closest('.sakai-resource-dropzone').addClass('sakai-resource-dropzone-active');
        }


    }).on('dragenter', function(event) {


    }).on('dragleave', function(event) {
        $(event.target).closest('.sakai-resource-dropzone').removeClass('sakai-resource-dropzone-active');

    }).on('drop', function(event) {
        event.preventDefault();

        $('.sakai-resource-dropzone-active').removeClass('sakai-resource-dropzone-active');

        if (!$(event.target).closest('.sakai-resource-dropzone').is(dragging)) {
            doMove(dragging, $(event.target).closest('.sakai-resource-dropzone'));
        }
    });
};


SakaiDrive.prototype.VIEW_URL = '/drive-tool/pdfjs/web/viewer.html?file=';
