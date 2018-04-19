// FIXME move to libary/src/morpheus-master/js/
(function($) {

    function JumpToTop() {
        this.init();
    };

    JumpToTop.prototype.init = function() {
        this.$link = $('<a>');
        this.$link.attr('id', 'jumptotop');
        this.$link.addClass('jumptotop');
        this.$link.addClass('hidden');
        this.$link.attr('title', 'Jump to top');

        var $image = $('<img>');
        $image.attr('src', '/library/image/jumptotop.png');
        $image.attr('aria-hidden', 'true');

        this.$link.html($image);
        this.hide();

        $(document.body).append(this.$link);

        this.bindEvents();
    };

    JumpToTop.prototype.bindEvents = function() {
        var self = this;

        $(window).on('scroll', function() {
            var magicOffset = $('.Mrphs-mainHeader').height();
            var containerHeight = $(window).height();
            var scrollHeight = $(document.body).height();
            var scrollOffset = Math.max(0, window.scrollY - magicOffset);

            if (scrollOffset == 0) {
                self.hide();
            } else {
                var opacity = Math.min(1, scrollOffset / containerHeight);
                self.show(opacity)
            }
        });

        self.$link.on('click', self.onClick);
    };

    JumpToTop.prototype.show = function(opacity) {
        if (opacity > 0) {
            this.$link.removeClass('hidden');
            this.$link.addClass('visible');
        }
        this.$link.css('opacity', opacity);
    };

    JumpToTop.prototype.hide = function() {
        this.$link.css('opacity', '0');
        this.$link.addClass('hidden');
        this.$link.removeClass('visible');
    };

    JumpToTop.prototype.onClick = function(event) {
        event.preventDefault();
        $('html, body').animate({scrollTop : 0}, 500);
    };


    new JumpToTop();
})($PBJQ);