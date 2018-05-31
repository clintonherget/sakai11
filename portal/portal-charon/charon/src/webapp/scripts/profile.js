// FIXME move to libary/src/morpheus-master/js/
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

    // FIXME remove when only supporting one profile template
    // destroy any existing qtips
    self.$link.qtip('destroy', true);

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
            }
        },
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

    if ($('#profile-drawer').length == 0) {
        $(document.body).append($("<div id='profile-drawer'>").hide());
    }

    this.$drawer = $('#profile-drawer');

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

    self.$drawer.html(html);

    if (!self.$drawer.is(':visible')) {
        self.$drawer.css('visibility', 'hidden');
        self.$drawer.show();
        self.reposition();
        self.$drawer.css('right', -self.$drawer.width() + 'px');
        self.$drawer.css('visibility', 'visible');
        self.$drawer.animate({
            right: 0
        }, 500);
    }
};

ProfileDrawer.prototype.attachEvents = function() {
    var self = this;

    function redraw() {
        if (self.$drawer.is(':visible')) {
            self.reposition();
        }
    };
    $(document).off('scroll', redraw).on('scroll', redraw);
    $(window).off('resize', redraw).on('resize', redraw);


    self.$drawer
        .off('click')
        .on('click', '.profile-connect-button', function(event) {
            ProfileHelper.requestFriend($(this).data('currentuserid'), $(this).data('connectionuserid'), function(text, status) {
                self.rerender();
            });
        })
        .on('click', '.profile-accept-button', function(event) {
            ProfileHelper.confirmFriendRequest($(this).data('currentuserid'), $(this).data('connectionuserid'), function(text, status) {
                self.rerender();
            });
        })
        .on('click', '.profile-ignore-button', function(event) {
            ProfileHelper.ignoreFriendRequest($(this).data('currentuserid'), $(this).data('connectionuserid'), function(text, status) {
                self.rerender();
            });
        })
        .on('click', '.profile-remove-connection-button', function(event) {
            ProfileHelper.removeFriend($(this).data('currentuserid'), $(this).data('connectionuserid'), function(text, status) {
                self.rerender();
            });
        })
        .on('click', '.close', function(event) {
            self.$drawer.animate({
               right: -self.$drawer.width() + 'px'
            }, 500, function() {
               self.$drawer.hide();
            });
        });
};

ProfileDrawer.prototype.rerender = function() {
    var self = this;

    self.show();
};

ProfileDrawer.prototype.reposition = function() {
    var self = this;
    var offset = $('.Mrphs-mainHeader').height() + $('.Mrphs-mainHeader').offset().top;
    var topScroll = $(document).scrollTop();
    if ($(document).scrollTop() < offset) {
        var magicNumber = offset - topScroll;
        self.$drawer.css('height', $(window).height() - magicNumber);
        self.$drawer.css('top', magicNumber);
    } else {
        self.$drawer.css('height', '100%');
        self.$drawer.css('top', '0');
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

    $container.on('click', '.profile-popup-link[data-userUuid],.profile-popup-link[data-useruuid]', callback);
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