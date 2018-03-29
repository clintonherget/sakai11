function ProfilePopup($link, userUuid, siteId) {
    this.$link = $PBJQ($link);

    if (userUuid) {
      this.userUuid = userUuid;
    } else { 
      this.userUuid = this.$link.data('userUuid') || this.$link.data('useruuid');
    }

    if (siteId) {
      this.siteId = siteId;
    } else { 
      this.siteId = this.$link.data('siteId') || this.$link.data('siteid');
    }

    if (!this.userUuid) {
      throw 'No userUuid provided';
    }
};

ProfilePopup.prototype.show = function() {
    var self = this;

    self.$link.qtip({
        position: {
            viewport: $PBJQ(window),
            at: 'bottom center',
            adjust: { method: 'flipinvert none'}
        },
        show: {
            ready: true,
            solo: true,
            delay: 0
        },
        style: {
            classes: 'qtip-shadow qtip-profile-popup',
            tip: {
               corner: true,
               offset: 20,
               mimic: 'center',
               width: 20,
               height: 8,
            }
        },
        hide: { event: 'click unfocus' },
        content: {
            ajax: {
                method: 'GET',
                url: "/direct/portal/" + self.userUuid + "/formatted",
                data: {
                  siteId: self.siteId,
                },
                dataType: 'html',
                once: false,
                accepts: {html: 'application/javascript'},
                cache: false,
                success: function(data, status) {
                    this.set('content.text', data);
                }
            },
            text: 'Loading...',
        },
//            
//            text: function (event, api) {
//                return $.ajax({ 
//                        method: 'GET',
//                        url: "/direct/portal/" + self.userUuid + "/formatted",
//                        data: {
//                          siteId: self.siteId,
//                        },
//                        cache: false
//                    })
//                    .then(function (html) {
//                        return html;
//                    }, function (xhr, status, error) {
//                        console.error('ProfilePopup.show', status + ': ' + error);
//                    });
//            }
//        },
        events: {
            hidden: function(event, api) {
                self.$link.qtip('destroy', true);
            },
            render: function(event, api) {
                self.addHandlers($(event.target));
            },
        }
    });
};

ProfilePopup.prototype.addHandlers = function($popup) {
    var self = this;
};

ProfilePopup.prototype.rerender = function() {
    var self = this;

    if (self.$link.data('qtip') && !self.$link.data('qtip').destroyed) {
        self.$link.qtip('api').destroy();
    }

    self.show();
};


function ProfileDrawer(userUuid, siteId) {
    this.userUuid = userUuid;
    this.siteId = siteId;

    if (!this.userUuid) {
        throw 'No userUuid provided';
    }

    this.attachEvents();
};

ProfileDrawer.prototype.show = function() {
    var self = this;

    $.ajax({ 
        method: 'GET',
        url: "/direct/portal/" + self.userUuid + "/drawer",
        data: {
          siteId: self.siteId,
        },
        cache: false
    })
    .then(function (html) {
        return self.render(html);
    }, function (xhr, status, error) {
        console.error('ProfileDrawer.show', status + ': ' + error);
    });
};

ProfileDrawer.prototype.render = function(html) {
    var self = this;

    var $wrapper = $('#profile-drawer');
    if ($wrapper.length == 0) {
        $wrapper = $('<div>').attr('id', 'profile-drawer').hide();
        $(document.body).append($wrapper);
    }
    $wrapper.html(html);

    if (!$wrapper.is(':visible')) {
        $wrapper.css('visibility', 'hidden');
        $wrapper.show();
        self.reposition();
        $wrapper.css('right', -$wrapper.width() + 'px');
        $wrapper.css('visibility', 'visible');
        $wrapper.animate({
            right: 0
        }, 500);
    }
};

ProfileDrawer.prototype.attachEvents = function() {
    var self = this;

    $(document.body).on('click', '#profile-drawer .close', function() {
        var $wrapper = $('#profile-drawer');
        $wrapper.animate({
            right: -$wrapper.width() + 'px'
        }, 500, function() {
            $wrapper.hide();
        });
    });

    function redraw() {
        var $wrapper = $('#profile-drawer');
        if ($wrapper.is(':visible')) {
            self.reposition();
        }
    };
    $(document).on('scroll', function(event) {
        redraw();
    });
    $(window).on('resize', function(event) {
        redraw();
    });


    $(document.body)
      .on('click', '#profile-drawer .profile-connect-button', function(event) {
          ProfileHelper.requestFriend($(this).data('currentuserid'), $(this).data('connectionuserid'), function(text, status) {
              self.rerender();
          });
      })
      .on('click', '#profile-drawer .profile-accept-button', function(event) {
          ProfileHelper.confirmFriendRequest($(this).data('currentuserid'), $(this).data('connectionuserid'), function(text, status) {
              self.rerender();
          });
      })
      .on('click', '#profile-drawer .profile-ignore-button', function(event) {
          ProfileHelper.ignoreFriendRequest($(this).data('currentuserid'), $(this).data('connectionuserid'), function(text, status) {
              self.rerender();
          });
      });
};

ProfileDrawer.prototype.rerender = function() {
    var self = this;

    self.show();
};

ProfileDrawer.prototype.reposition = function() {
    var $wrapper = $('#profile-drawer');
    var offset = $('.Mrphs-mainHeader').height() + $('.Mrphs-mainHeader').offset().top;
    var topScroll = $(document).scrollTop();
    if ($(document).scrollTop() < offset) {
        var magicNumber = offset - topScroll;
        $wrapper.css('height', $(window).height() - magicNumber);
        $wrapper.css('top', magicNumber);
    } else {
        $wrapper.css('height', '100%');
        $wrapper.css('top', '0');
    }
};


var ProfileHelper = {};
ProfileHelper.registerPopupLinks = function($container) {
    if (!$container) {
        $container = $PBJQ(document.body);
    }

    function callback(event) {
        event.preventDefault();
        event.stopPropagation();

        var pp = new ProfilePopup($PBJQ(this));
        pp.show();
    };

    $container.on('click', '.profile-popup[data-userUuid],.profile-popup[data-useruuid]', callback);
};

ProfileHelper.registerDrawerLinks = function() {
    function callback(event) {
        event.preventDefault();
        event.stopPropagation();
    
        var pd = new ProfileDrawer($PBJQ(this).data('useruuid'), $PBJQ(this).data('siteid'));
        pd.show();
    };

    $PBJQ(document.body).on('click', '.profile-link[data-useruuid]', callback);
};

ProfileHelper.CONNECTION_NONE = 0;
ProfileHelper.CONNECTION_REQUESTED = 1;
ProfileHelper.CONNECTION_INCOMING = 2;
ProfileHelper.CONNECTION_CONFIRMED = 3;

ProfileHelper.friendStatus = function(requestorId, friendId) {
    var status = null;

    $PBJQ.ajax({
        url : "/direct/profile/" + requestorId + "/friendStatus.json?friendId=" + friendId,
          dataType : "json",
          async : false,
      cache: false,
      success : function(data) {
          status = data.data;
      },
      error : function() {
          status = -1;
      }
    });

    return status;
};

ProfileHelper.requestFriend = function(requestorId, friendId, callback) {

    if (callback == null) {
        callback = $PBJQ.noop;
    }

    jQuery.ajax( {
        url : "/direct/profile/" + requestorId + "/requestFriend?friendId=" + friendId,
        dataType : "text",
        cache: false,
        success : function(text,status) {
            callback(text, status);
        }
    });

    return false;
};

ProfileHelper.confirmFriendRequest = function(requestorId, friendId, callback) {

    if (callback == null) {
        callback = $PBJQ.noop;
    }

    jQuery.ajax( {
        url : "/direct/profile/" + requestorId + "/confirmFriendRequest?friendId=" + friendId,
        dataType : "text",
        cache: false,
        success : function(text,status) {
            callback(text, status);
        }
    });

    return false;
}

ProfileHelper.removeFriend = function(removerId, friendId, callback) {

    if (callback == null) {
        callback = $PBJQ.noop;
    }

    jQuery.ajax( {
        url : "/direct/profile/" + removerId + "/removeFriend?friendId=" + friendId,
        dataType : "text",
        cache: false,
        success : function(text,status) {
            callback(text, status);
        }
    });

    return false;
};

ProfileHelper.ignoreFriendRequest = function(removerId, friendId, callback) {

    if (callback == null) {
        callback = $PBJQ.noop;
    }

    jQuery.ajax( {
        url : "/direct/profile/" + removerId + "/ignoreFriendRequest?friendId=" + friendId,
        dataType : "text",
        cache: false,
        success : function(text,status) {
            callback(text, status);
        }
    });

    return false;
}

$PBJQ(document).ready(function() {
  ProfileHelper.registerPopupLinks();
  ProfileHelper.registerDrawerLinks();
});