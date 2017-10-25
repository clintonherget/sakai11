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
    var lastSelectedRow = null;

    function toggleRow($tr, force_active) {
        if (force_active) {
            $tr.addClass('active');
        } else {
            $tr.toggleClass('active');
        }
        if ($tr.is('.active')) {
            lastSelectedRow = $tr;
        } else if (lastSelectedRow[0] == $tr[0]) {
            lastSelectedRow = null;
        }
    };

    function clearAll() {
        $('.sakai-resources-table tbody tr.active').removeClass('active');
        lastSelectedRow = null;
    };

    function selectRowsBetweenIndexes(a, b) {
        var min = Math.min(a, b);
        var max = Math.max(a, b);

        for (var i = min; i <= max; i++) {
            var $row = $($('.sakai-resources-table tbody tr').get(i));
            toggleRow($row, true);
        }
    };

    $('.sakai-resources-table').on('mousedown', 'tbody tr', function(event) {
        var $tr = $(event.target).closest('tr');

        if (event.ctrlKey || event.metaKey) {
            toggleRow($tr);
        } else if (event.shiftKey) {
            if (lastSelectedRow) {
                selectRowsBetweenIndexes(lastSelectedRow.index(), $tr.index());
            } else {
                toggleRow($tr);
            }
        } else {
            toggleRow($tr, true);
        }
    }).on('click', 'tbody tr', function(event) {
        var $tr = $(event.target).closest('tr');

        if (!event.ctrlKey && !event.metaKey && !event.shiftKey) {
            clearAll();
            toggleRow($tr, true);
        }
    }).on('dblclick', 'tbody tr', function(event) {
        var $tr = $(this).closest('tr');

        if ($(event.target).is('a')) {
            return true;
        }

        $tr.find('td.name a')[0].click();

        return false;
    });;
};


SakaiDrive.prototype.setupDragAndDrop = function() {
    function doMove(target) {
        var $form = $('form#move-form');
        $form.empty();

        $.each($('.sakai-resources-table tbody tr.active'), function() {
            var $sourceInput = $('<input type="hidden" name="source[]">');
            $sourceInput.val($(this).find('[data-path]').data('path'));
            $form.append($sourceInput);
        });

        var $targetInput = $('<input type="hidden" name="target">');
        $targetInput.val($(target).find('[data-path]').data('path'));
        $form.append($targetInput);

        $form.submit();
    };

    // setup draggables
    $('.sakai-resources-table tbody tr').addClass('sakai-resource-draggable');

    $('.sakai-resource-draggable').draggable({
        opacity: 0.8,
        helper: function(event) {
            var $active = $('.sakai-resources-table tbody tr.active');
            var $helper = $('<div>');

            if ($active.length > 1) {
                var $label = $('<div>').append('<i class="fa fa-files-o" aria-hidden="true"></i> ').append($active.length + ' items');
                $label.addClass('name');
                $label.width(200);
                $helper.append($label);
            } else {
                var $tr = $(event.target).closest('.sakai-resource-draggable');
                var $name = $('<div>').append($tr.find('td.name').html());
                $name.addClass('name');
                $name.width($active.find('td.name a').width() + 50);
                $helper.append($name);
            }

            $helper.addClass('sakai-resource-drag-helper');
            $helper.width($($active.get(0)).width());
            $helper.height($($active.get(0)).height());
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
            return draggable.is('.sakai-resource-draggable');
        },
        hoverClass: 'sakai-resource-dropzone-active',
        drop: function(event, ui) {
            doMove(event.target);
        },
        tolerance: 'pointer',
    });
};


SakaiDrive.prototype.VIEW_URL = '/drive-tool/pdfjs/web/viewer.html?file=';
