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
                        console.error('content.text', status + ': ' + error);
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

$PBJQ(document).ready(function() {
  ProfileHelper.registerPopupLinks();
});