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
        } else if (!$tr.is('.active')) {
            clearAll();
            toggleRow($tr);
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


function GoogleDrive(rootElt, baseURL, options) {
  this.root = rootElt;
  this.baseURL = baseURL;
  this.options = options;

  this.scrollContainer = this.root.find('.scroll-container');
  this.list = this.root.find('.file-list');

  this.nextPageToken = null;

  this.currentSearchTerm = '';

  this.loadThresholdPx = parseInt(this.scrollContainer.height()) * 4;
  this.currentlyLoading = false;

  this.setupList();
  this.setupScrollHandling();

  if (options.enable_search) {
    this.search = this.root.find('.file-search');
    this.setupSearch();
  }

  this.getFiles();
};

GoogleDrive.prototype.setupList = function() {
  this.listOverlay = $('<div class="list-overlay" />');

  this.listOverlay
      .css('position', 'absolute')
      .css('top', this.scrollContainer.position().top)
      .css('left', this.scrollContainer.position().left);

  this.root.append(this.listOverlay);
}

GoogleDrive.prototype.showOverlay = function () {
  if (this.list.find('li').length == 0) {
    /* Nothing to overlay */
    return;
  }
  this.listOverlay
      .css('width', this.scrollContainer.width())
      .css('height', this.scrollContainer.height())
      .show();
};

GoogleDrive.prototype.hideOverlay = function () {
  this.listOverlay.hide();
};

GoogleDrive.prototype.showNoMatches = function () {
  this.root.find('.no-matches-msg').show();
};

GoogleDrive.prototype.hideNoMatches = function () {
  this.root.find('.no-matches-msg').hide();
};

GoogleDrive.prototype.setupScrollHandling = function() {
  var self = this;

  self.scrollContainer.on('scroll', function () {
    if (self.currentlyLoading || !self.nextPageToken) {
      /* Nothing needs doing */
      return true;
    }

    var scrollPosition = self.scrollContainer.scrollTop() + self.scrollContainer.height();

    if (scrollPosition >= (self.list.height() - self.loadThresholdPx)) {
      /* Load next page */
      self.currentlyLoading = true;
      self.getFiles(self.nextPageToken, {
        complete: function () {
          self.currentlyLoading = false;
        }
      });
    }

    return true;
  });
};

GoogleDrive.prototype.setupSearch = function() {
  var self = this;
  var updateTimer = null;

  self.search.on('input', function () {
    if (updateTimer) {
      clearTimeout(updateTimer);
    }

    updateTimer = setTimeout(function () {
      updateTimer = null;
      self.handleSearchChange();
    }, 250);
  });
};

GoogleDrive.prototype.handleSearchChange = function () {
  var self = this;

  // ensure recent tab is visible
  this.root.find('.google-drive-menu a[href="#googledriverecent"]').tab('show');

  console.log("handling search change");

  if (self.currentlyLoading) {
    console.log("defer...");
    /* Defer handling until we finish loading */
    setTimeout(function () {
      self.handleSearchChange();
    }, 200);

    return true;
  }

  var query = self.search.val();

  if (query == this.currentSearchTerm) {
    /* Nothing to do anyway! */
    console.log("Nothing to do");
    return true;
  }

  console.log("here we go!");
  self.currentSearchTerm = (query || '');
  self.nextPageToken = null;

  self.showOverlay();

  self.getFiles(null, {
    replaceList: true,
    complete: function () {
      self.hideOverlay();
    }
  });

  return true;
};

GoogleDrive.prototype.getFiles = function(pageToken, options) {
  var self = this;
  var fileTemplate = TrimPath.parseTemplate($("#fileTemplate").html().trim().toString());

  if (!pageToken) { pageToken = ''; }
  if (!options) { options = {} }
  if (!options.complete) { options.complete = $.noop; }
  if (!options.data) { options.data = {}; }

  var query = (self.currentSearchTerm || '');

  var data = {};

  if (pageToken != '') {
    data = self._currentPageData;
    data.pageToken = pageToken;
  };

  if (self.options.enable_search) {
    data.q = query;
  }

  self._currentPageData = $.extend({}, data, options.data);

  $.getJSON(this.baseURL + this.options.path,
            self._currentPageData,
            function(json) {
              self.hideNoMatches();

              if (options.replaceList) {
                self.list.empty();
              }

              $.each(json.files, function(index, page) {
                var html = fileTemplate.process(page);
                self.list.append(html);
                self.nextPageToken = json.nextPageToken;
              });

              if (json.files.length == 0) {
                self.showNoMatches();
              }

              options.complete();
            });
};

GoogleDrive.prototype.refreshListForFolder = function(folderId) {
  this.getFiles(null, {
    replaceList: true,
    data: {
      folderId: folderId,
    }
  });
};


function GoogleDriveModal($modal, baseURL) {
  this.$modal = $modal;
  this.baseURL = baseURL;

  this.init();
};

GoogleDriveModal.prototype.init = function() {
  var self = this;

  $(window).resize(function() {
    self.resizeGoogleModal();
  });

  self.$modal.on('show.bs.modal', function () {
    $.ajax(baseURL + "show-google-drive",
           {
             type: "GET",
             contentType: "html",
           }).success(function (content) {
             $('#google-drive-modal .modal-body').html(content);

             self.setupTabs();

             // setup form submit
             $('#google-drive-modal .modal-footer .btn-primary').on('click', function() {
               $('#google-drive-modal .modal-body form.google-drive-add-selected-form:visible').submit();
             });
           });

    return true;
  });

  self.$modal.on('shown.bs.modal', function () {
    self.resizeGoogleModal();
  });
};

GoogleDriveModal.prototype.resizeGoogleModal = function() {
  if (this.$modal.is(':visible')) {
    this.$modal.find('.modal-body').height($(window).height() - this.$modal.find('.modal-header').height() - this.$modal.find('.modal-footer').height() - 150);
    this.$modal.find('.tab-content').height(this.$modal.find('.modal-body').height() - (this.$modal.find('.tab-content').offset().top - this.$modal.find('.modal-body').offset().top))
    this.$modal.find('.scroll-container').height(this.$modal.find('.tab-content').height() - (this.$modal.find('.tab-pane.active .breadcrumb').height() || 0));
  }
};

GoogleDriveModal.prototype.setupTabs = function() {
    var self = this;

    // bootstap tabs please
    self.$modal.find('.google-drive-menu').tab();

    // Recent/Search
    new GoogleDrive(self.$modal.find('#googledriverecent'), baseURL, {
      enable_search: true,
      path: '/drive-data',
    });

    self.$modal.find('.google-drive-menu a[href="#googledriverecent"]').on('show.bs.tab', function() {
      // do nothing! for the moment anyway
    });

    // My Drive
    self.$modal.find('.google-drive-menu a[href="#googledrivehome"]').on('show.bs.tab', function() {
      // load the drive home
        if (!self._homeLoaded) {
          // load my drive (for root context)
          var googleDrive = new GoogleDrive($('#googledrivehome'), baseURL, {
            enable_search: false,
            path: '/my-drive-data',
          });


          $("#googledrivehome").on('click', '.google-drive-folder, .breadcrumb a', function() {
              var $link = $(this);
              var text = $link.text();
              var folder = $link.data('id');
              $("#googledrivehome .file-list").empty();

              if ($link.closest('.breadcrumb').length == 1) {
                  $link.closest('li').nextAll().remove();
                  $link.closest('li').addClass('active');
              } else {
                  var breadcrumb = $('<li>');
                  var a = $('<a>').attr('href','#').data('id', folder).text(text);
                  breadcrumb.append(a);
                  breadcrumb.addClass('active');
                  $("#googledrivehome .breadcrumb .active").removeClass('active');
                  $("#googledrivehome .breadcrumb").append(breadcrumb);
              }

              googleDrive.refreshListForFolder(folder);
          });

          self._homeLoaded = true;
       }
    });
};