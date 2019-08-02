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
      .css('height', this.scrollContainer.closest('.tab-pane').height())
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

GoogleDrive.prototype.showAuthError = function () {
  this.root.find('.auth-error').show();
};

GoogleDrive.prototype.showError = function (errorText) {
  this.root.find('.error-messages').text(errorText).show();
};

GoogleDrive.prototype.hideErrors = function () {
  this.root.find('.auth-error, .error-messages').hide();
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
              self.hideErrors();

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
            }).fail(function( jqxhr, textStatus, error ) {
                if (jqxhr.status >= 400 && jqxhr.status < 500) {
                  self.showAuthError();
                } else {
                  self.showError(jqxhr.responseText);
                }
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



function GoogleDriveContainer($container, baseURL, dialog) {
  this.$container = $container;
  this.baseURL = baseURL;
  this.dialog = dialog;

  if (this.$container.find('.google-drive').length > 0) {
    this.init();
  }
};

GoogleDriveContainer.prototype.init = function() {
  var self = this;

  $(window).resize(function() {
    self.resizeGoogleContainer();
  });

  self.setupTabs();
  self.setupForm();

  self.resizeGoogleContainer();

  self.$container.on('click', '.file-list :checkbox', function(event) {
    if ($(this).is(':checked')) {
      $(this).closest('tr').addClass('active');
    } else {
      $(this).closest('tr').removeClass('active');
    }
    self.handleCheckboxChange();
  });

  $('#resetGoogleOAuth').on('click', function(event) {
    $.ajax({
      url: '/direct/google-drive/reset-oauth',
      success: function() {
        self.dialog.reload();
      }
    });
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

  var $form = $('form.google-drive-select-item-form');

  var $button = $('#addSelectedGoogleItems');
  $button.on('click', function() {
    $form.submit();
  });

  $form.on('submit', function(event) {
    event.preventDefault();

    var files = self.getSelectedFilesIds();

    $form.find(':hidden[name="googleitemid[]"]').remove();

    for (var i=0; i<files.length; i++) {
      var $hidden = $('<input type="hidden">').attr('name', 'googleitemid[]').val(files[i]);
      $form.append($hidden);
    }

    if (files.length == 0) {
      return false;
    }

    $.ajax({
      url: $form.attr('action'),
      data: $form.serialize(),
      type: 'post',
      dataType: 'html',
      success: function(html) {
        self.dialog.setContents(html);
        new GoogleDriveForm(self.dialog);
      },
      error: function(message) {
        self.$form.find('.error-messages').show();
        self.$form.find('.error-messages').text(message.responseText);
      }
    });
  });
};


GoogleDriveContainer.prototype.resizeGoogleContainer = function() {
  var containerHeight = $("#googleDriveModal").closest('.ui-dialog').height() - 210;
  this.$container.find('.scroll-container').height(containerHeight);
  this.$container.find('.google-drive-menu').height(containerHeight);
};

GoogleDriveContainer.prototype.setupTabs = function() {
    var self = this;

    // bootstap tabs please
    self.$container.find('.google-drive-menu').tab();

    self._currentTab = 'googledriverecent';

    // Recent/Search
    self.recentDrive = new GoogleDrive(self.$container.find('#googledriverecent'), self.baseURL, {
      path: '/drive-data',
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
            path: '/my-drive-data',
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
            path: '/starred-drive-data',
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


function GoogleDriveForm(dialog) {
  var self = this;
  self.dialog = dialog;
  self.$form = $('form.google-item-form');
  self.$form.on('submit', function(event) {
    event.preventDefault();

    $.ajax({
      url: self.$form.attr('action'),
      data: self.$form.serialize(),
      type: 'post',
      dataType: 'html',
      success: function(html) {
        self.$form.find('.error-messages').hide();
        self.dialog.setContents(html);
        new GoogleDriveForm(self.dialog);
      },
      error: function(message) {
        self.$form.find('.error-messages').show();
        self.$form.find('.error-messages').text(message.responseText);
      }
    });
  });

  $('#returnToDrive, #returnToChangeSelection').on('click', function(event) {
    event.preventDefault();
    self.dialog.reload();
  });
}


function GoogleDriveItemDialog() {
  var self = this;

  self.id = "googleDriveModal";
  self.showDialog();
  self.bindEvents();
};

GoogleDriveItemDialog.prototype.showDialog = function() {
  var self = this;

  $("<div id='" + self.id + "' class='drive-body'>Loading...</div>").appendTo(document.body);
  $("#googleDriveModal").dialog({
    modal: true,
    resizable: false,
    open: function() {
      $(document.body).css({
        overflow: 'hidden',
      });
    },
    close: function() {
      $(document.body).css({
        overflow: '',
      });
      $("#googleDriveModal").dialog('destroy');
      $("#googleDriveModal").remove();
    }
  });
  $("#googleDriveModal").closest('.ui-dialog').addClass('ui-dialog-full-screen').addClass('google-drive-dialog');
};

GoogleDriveItemDialog.prototype.bindEvents = function() {
  var self = this;

  $(window).on('resize', function () {
      var width = Math.min(1200, $(window).width() - 100);
      var leftOffset = parseInt(($(window).width() - width) / 2);

      $('.ui-dialog.ui-dialog-full-screen').css({
        'width': width,
        'height': $(window).height() - 100,
        'left': leftOffset + 'px',
        'top':'50px',
        'position': 'fixed',
        'z-index': 1000,
      });

      if ($("#googleDriveModal .google-dialog-content").length > 0) {
        var contentHeight = $("#googleDriveModal").closest('.google-drive-dialog').height() - $("#googleDriveModal .google-dialog-header").height() - 60;
        $("#googleDriveModal .google-dialog-content").height(contentHeight)
      }
    });

    $(window).trigger('resize');

    $('#googleDriveModal').on('click', '.close-google-dialog', function() {
      $('#googleDriveModal').dialog('close');
    });

    $('#googleDriveModal').on('click', '#cancelSelectedGoogleItems', function() {
      $('#googleDriveModal').dialog('close');
    });

};

GoogleDriveItemDialog.prototype.load = function(url, data, onSuccess) {
  var self = this;

  self._reload_data = {
    url: url,
    data: data,
    onSuccess: onSuccess
  };

  $.ajax({
    url: url,
    type: 'get',
    dataType: 'html',
    data: data,
    success: function(html) {
      self.setContents(html);
      onSuccess();
    },
  });
};

GoogleDriveItemDialog.prototype.setContents = function(html) {
  $("#googleDriveModal").html(html);
  $(window).trigger('resize');
};

GoogleDriveItemDialog.prototype.reload = function() {
  var self = this;
  if (self._reload_data) {
    self.load(self._reload_data.url, self._reload_data.data, self._reload_data.onSuccess);
  } else {
    throw "Nothing to reload";
  }
};

var GOOGLE_ITEM_ROUTES = {
  show: window.location.pathname + "/google-drive/show-google-drive",
  edit: window.location.pathname + "/google-drive/edit-google-item",
};

function ResourceListWithGoogleItems() {
  var self = this;

  // Bootstrap our custom Add Google item workflow onto the Create Google Item menu item
  $("table.resourcesList a[onclick*='org.sakaiproject.content.types.GoogleDriveItemType:create']").removeAttr('onclick').on('click', function(event) {
    event.preventDefault();

    self.dialog = new GoogleDriveItemDialog();
    self.collectionId = $(this).closest('tr').find(':checkbox[name=selectedMembers]').val();
    if (self.collectionId == null) {
      // no checkbox! ok, so get the collectionId from the form
      self.collectionId = $(this).closest('form').find(':hidden[name=collectionId]').val();
    }
    self.dialog.load(GOOGLE_ITEM_ROUTES.show,
                     {
                       'collectionId': self.collectionId
                     },
                     function() {
                       new GoogleDriveContainer($("#googleDriveModal"), window.location.pathname + "/google-drive", self.dialog);
                     });
  });

  // Insert an Edit Details action for google items
  $("table.resourcesList a[onclick*='org.sakaiproject.content.types.GoogleDriveItemType:delete']").each(function() {
    var $deleteAction = $(this);
    var $insertAction = $('<li><a href="javascript:void(0);">Edit Details</a></li>');
    var resourceId = $deleteAction.closest('tr').find(':checkbox[name=selectedMembers]').val();

    $deleteAction.closest('li').before($insertAction);

    $insertAction.on('click', function() {
      self.dialog = new GoogleDriveItemDialog();
      self.dialog.load(GOOGLE_ITEM_ROUTES.edit,
                       {
                         'resourceId': resourceId
                       },
                       function() {
                         new GoogleDriveForm(self.dialog);
                       });
    });
  });
};
