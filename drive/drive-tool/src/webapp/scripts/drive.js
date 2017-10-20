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

    // setup draggables
    $('.sakai-resources-table tbody tr').addClass('sakai-resource-draggable');

    $('.sakai-resource-draggable').draggable({
        opacity: 0.8,
        helper: function(event) {
            var $tr = $(event.target).closest('.sakai-resource-draggable');
            var $helper = $('<div>');
            var $name = $('<div>').append($tr.find('td.name').html());
            $name.addClass('name');
            $name.width($tr.find('td.name a').width() + 50);
            $helper.append($name);
            $helper.addClass('sakai-resource-drag-helper');
            $helper.width($tr.width());
            $helper.height($tr.height());
            return $helper;
        },
        cursorAt: {
            left: 5,
        },
        start: function(event, ui) {
            $(ui.helper).animate({
                'width': $(ui.helper).find('> .name').width()
            });
        }
    });

    // setup droppables
    $('.sakai-resources .sakai-resources-breadcrumbs .breadcrumb-item:not(.active)').addClass('sakai-resource-dropzone');
    $('.sakai-resources .sakai-resources-table .drive-folder').each(function() {
        $(this).closest('tr').addClass('sakai-resource-dropzone');
    });

    $('.sakai-resource-dropzone').droppable({
        accept: function(draggable) {
            console.log(draggable.is('.sakai-resource-draggable'));
            return draggable.is('.sakai-resource-draggable');
        },
        hoverClass: 'sakai-resource-dropzone-active',
        drop: function(event, ui) {
            doMove(ui.draggable[0], event.target);
        },
        over: function(event, ui) {
            console.log('over', event);
        },
        tolerance: 'touch',
    });
};


SakaiDrive.prototype.VIEW_URL = '/drive-tool/pdfjs/web/viewer.html?file=';
