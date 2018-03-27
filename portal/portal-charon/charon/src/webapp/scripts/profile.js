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
            text: function (event, api) {
                return $.ajax({ 
                        method: 'GET',
                        url: "/direct/portal/" + self.userUuid + "/formatted",
                        data: {
                          siteId: self.siteId,
                        },
                        cache: false
                    })
                    .then(function (html) {
                        return html;
                    }, function (xhr, status, error) {
                        console.error('ProfilePopup.show', status + ': ' + error);
                    });
            }
        },
        events: {
            hidden: function(event, api) {
                $(event.target).remove();
            }
        }
    });
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

    $(document).on('scroll', function(event) {
        var $wrapper = $('#profile-drawer');
        if ($wrapper.is(':visible')) {
            self.reposition();
        }
    });
};

ProfileDrawer.prototype.reposition = function() {
    var $wrapper = $('#profile-drawer');
    var offset = $('.Mrphs-mainHeader').height();
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

$PBJQ(document).ready(function() {
  ProfileHelper.registerPopupLinks();
  ProfileHelper.registerDrawerLinks();
});