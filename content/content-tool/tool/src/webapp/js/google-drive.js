function GoogleDrive(rootElt, baseURL, options, onLoading, onLoaded) {
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

  this.search = this.root.find('.file-search');
  this.setupSearch();

  this.onLoading = onLoading || $.noop;
  this.onLoaded = onLoaded || $.noop;

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

// FIXME now that scrollbar is on the window, we need to rework this one :(
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

GoogleDrive.prototype.clearSearch = function() {
  this.currentSearchTerm = '';
  this.search.val('');
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
  // this.root.closest('.google-drive').find('.google-drive-menu a[href="#googledriverecent"]').tab('show');

  if (!self.root.is(':visible')) {
    return true;
  }

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
    },
    data: $.extend({}, self._currentPageData || {}, {
      q: self.currentSearchTerm,
    }),
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

  var data = {};

  if (pageToken != '') {
    data = self._currentPageData;
    data.pageToken = pageToken;
  };

  self._currentPageData = $.extend({}, data, options.data);

  self.onLoading();

  $.getJSON(self.baseURL + this.options.path,
            self._currentPageData,
            function(json) {
              self.hideNoMatches();

              if (options.replaceList) {
                self.list.empty();
              }

              $.each(json.files, function(index, page) {
                var html = fileTemplate.process(page);
                self.list.append(html);
              });

              self.nextPageToken = json.nextPageToken;

              if (json.files.length == 0) {
                self.showNoMatches();
              }

              options.complete();

              self.onLoaded();
            });
};

GoogleDrive.prototype.refreshListForFolder = function(folderId) {
  var self = this;

  self.getFiles(null, {
    replaceList: true,
    data: {
      folderId: folderId,
    },
    complete: function() {
      self.hideOverlay();
    }
  });
};



function GoogleDriveContainer($container, baseURL) {
  this.$container = $container;
  this.baseURL = baseURL;

  this.init();
};

GoogleDriveContainer.prototype.init = function() {
  var self = this;

  $(window).resize(function() {
    self.resizeGoogleContainer();
  });

  self.setupTabs();
  self.setupForm();

  self.$container.on('shown.bs.modal', function () {
    self.resizeGoogleContainer();
  });

  self.$container.on('click', '.file-list :checkbox', function(event) {
    if ($(this).is(':checked')) {
      $(this).closest('li').addClass('active');
    } else {
      $(this).closest('li').removeClass('active');
    }
    self.handleCheckboxChange();
  });
};

GoogleDriveContainer.prototype.getSelectedFilesIds = function() {
  var self = this;

  var files = [];

  self.$container.find('.file-list :checkbox:checked:visible').each(function() {
    files.push($(this).val());
  });

  return files;
};

GoogleDriveContainer.prototype.setupForm = function() {
  var self = this;

  var $form = $('#addGoogleItemsForm');

  var $button = $('#addSelectedGoogleItems');
  $button.on('click', function() {
    $form.submit();
  });

  $form.on('submit', function() {
    var files = self.getSelectedFilesIds();

    $form.find(':hidden[name="googleitemid[]"]').remove();

    for (var i=0; i<files.length; i++) {
      var $hidden = $('<input type="hidden">').attr('name', 'googleitemid[]').val(files[i]);
      $form.append($hidden);
    }

    if (files.length == 0) {
      return false;
    }

    return true;
  });
};


GoogleDriveContainer.prototype.resizeGoogleContainer = function() {
//  if (this.$container.is(':visible')) {
//    this.$container.find('.tab-content').height($(window).height() - this.$container.find('.tab-content').position().top - 150);
//    this.$container.find('.scroll-container').height(this.$container.find('.tab-content').height() - (this.$container.find('.tab-pane.active .breadcrumb').height() || 0));
//  }
};

GoogleDriveContainer.prototype.setupTabs = function() {
    var self = this;

    // bootstap tabs please
    self.$container.find('.google-drive-menu').tab();

    self._currentTab = 'googledriverecent';

    // Recent/Search
    self.recentDrive = new GoogleDrive(self.$container.find('#googledriverecent'), self.baseURL, {
      path: '/drive-data?mode=recent',
    }, $.proxy(self.onLoading, self), $.proxy(self.onLoaded, self));

    self.$container.find('.google-drive-menu a[href="?mode=recent#googledriverecent"]').on('show.bs.tab', function() {
        self._currentTab = 'googledriverecent';
    });

    // My Drive
    self.myDrive = null;
    self.$container.find('.google-drive-menu a[href="#googledrivehome"]').on('show.bs.tab', function() {
        self._currentTab = 'googledrivehome';

        // load the drive home
        if (self.myDrive == null) {
          // load my drive (for root context)
          self.myDrive = new GoogleDrive($('#googledrivehome'), self.baseURL, {
            path: '/drive-data?mode=home',
          }, $.proxy(self.onLoading, self), $.proxy(self.onLoaded, self));


          $("#googledrivehome").on('click', '.google-drive-folder, .breadcrumb a', function() {
              var $link = $(this);
              var text = $link.text();
              var folder = $link.data('id');

              self.myDrive.showOverlay();

              self.myDrive.clearSearch();

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

              self.myDrive.refreshListForFolder(folder);
          });
       }
    });

    // Starred
    self.starredDrive = null;
    self.$container.find('.google-drive-menu a[href="#googledrivestarred"]').on('show.bs.tab', function() {
        self._currentTab = 'googledrivestarred';

        // load the drive home
        if (self.starredDrive == null) {
          // load my drive (for root context)
          self.starredDrive = new GoogleDrive($('#googledrivestarred'), self.baseURL, {
            path: '/drive-data?mode=starred',
          }, $.proxy(self.onLoading, self), $.proxy(self.onLoaded, self));


          $("#googledrivestarred").on('click', '.google-drive-folder, .breadcrumb a', function() {
              var $link = $(this);
              var text = $link.text();
              var folder = $link.data('id');

              self.starredDrive.showOverlay();

              self.starredDrive.clearSearch();

              if ($link.closest('.breadcrumb').length == 1) {
                  $link.closest('li').nextAll().remove();
                  $link.closest('li').addClass('active');
              } else {
                  var breadcrumb = $('<li>');
                  var a = $('<a>').attr('href','#').data('id', folder).text(text);
                  breadcrumb.append(a);
                  breadcrumb.addClass('active');
                  $("#googledrivestarred .breadcrumb .active").removeClass('active');
                  $("#googledrivestarred .breadcrumb").append(breadcrumb);
              }

              self.starredDrive.refreshListForFolder(folder);
          });
       }
    });
};

GoogleDriveContainer.prototype.onLoading = function() {
  this._spinner = $('<i class="loading fa fa-circle-o-notch" aria-hidden="true"></i>');
  this.$container.find('.google-drive-menu a[href="#'+this._currentTab+'"]').append(this._spinner);
}

GoogleDriveContainer.prototype.handleCheckboxChange = function() {
  if (this.$container.find(':checkbox:checked').length > 0) {
    $('#addSelectedGoogleItems').prop('disabled', false).removeClass('disabled');
  } else {
    $('#addSelectedGoogleItems').prop('disabled', true).addClass('disabled');
  }
};

GoogleDriveContainer.prototype.onLoaded = function() {
  if (this._spinner) {
    this._spinner.remove();
    this._spinner = undefined;
  }
  this.handleCheckboxChange();
}