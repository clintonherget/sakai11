(function() {
    var LESSONS_SUBPAGE_NAVIGATION_LABELS = {
        expand:                     'Expand and show sub pages',
        collapse:                   'Collapse and hide sub pages',
        open_top_level_page:        'Click to open top-level page',
    };

    var LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH = 90;

    function LessonsSubPageNavigation(data, currentPageId) {
        this.data = data;
        this.currentPageId = currentPageId;
        this.has_current = false;
        this.page_id_to_submenu_item = {};
        this.setup();
    };

    LessonsSubPageNavigation.prototype.setup = function() {
        var self = this;

        for (var page_id in self.data) {
            if (self.data.hasOwnProperty(page_id)) {
                var sub_pages = self.data[page_id];
                self.render_subnav_for_page(page_id, sub_pages);
            }
        }
    };

    LessonsSubPageNavigation.prototype.render_subnav_for_page = function(page_id, sub_pages) {
        var self = this;

        var submenu_id = "lessonsSubMenu_" + page_id;

        var $menu = document.querySelector('#toolMenu a[href$="/tool/'+page_id+'"], #toolMenu [href$="/tool-reset/'+page_id+'"]');
        var $li = $menu.parentElement;

        var $submenu = document.createElement('ul');
        $submenu.classList.add('lessons-sub-page-menu');
        $submenu.setAttribute('aria-hidden', true);
        $submenu.style.display = 'none';
        $submenu.id = submenu_id;

        sub_pages.forEach(function(sub_page) {
            var $submenu_item = document.createElement('li');
            var $submenu_action = document.createElement('a');

            $submenu_action.href = self.build_sub_page_url_for(sub_page);
            $submenu_action.innerText = sub_page.name;
            $submenu_action.setAttribute('data-sendingPage', sub_page.sendingPage);

            var title_string = sub_page.name;
            if (title_string.length < LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH - 20) { // only show description if there's room
                if (sub_page.description) {
                  if (sub_page.description.length > (LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH - title_string.length)) {
                    title_string += " - " + sub_page.description.substring(0, LESSONS_SUBPAGE_TOOLTIP_MAX_LENGTH) + "...";
                  } else {
                    title_string += " - " + sub_page.description;
                  }
                }
            }
            $submenu_action.title = title_string;

            $submenu_item.appendChild($submenu_action);

            $submenu.appendChild($submenu_item);

            if (sub_page.sendingPage === self.currentPageId) {
                $li.classList.add('is-parent-of-current');
                $submenu_item.classList.add('is-current');
                self.has_current = true;
            }

            self.page_id_to_submenu_item[sub_page.sendingPage] = $submenu_item;
        });

        $li.appendChild($submenu);
        self.setup_parent_menu($li, $menu, submenu_id);
    };


    LessonsSubPageNavigation.prototype.expand = function($expandMe) {
        $expandMe.addClass('sliding-down');
        $expandMe.hide().show(0);
        $expandMe.find('.lessons-sub-page-menu').slideDown(500, function() {
            var $submenu = $(this);
            $expandMe.hide().show(0); // force a redraw so hover states are respected
            // and to avoid flash of the goto link pause to ensure this redraw...
            setTimeout(function() {
                $expandMe.removeClass('sliding-down');
                $expandMe.addClass('expanded');

                var $expandMeIcon = $expandMe.find('.lessons-expand-collapse-icon')
                var $expandMeLink = $expandMe.find('> a');
                var $expandMeTitle = $expandMe.find('.lessons-top-level-title');

               $expandMeIcon
                    .attr('tabindex', 0)
                    .attr('title', LESSONS_SUBPAGE_NAVIGATION_LABELS.collapse)
                    .attr('aria-controls', $expandMeLink.attr('aria-controls'))
                    .attr('aria-expanded', true)
                    .attr('aria-hidden', false);

                $expandMeLink
                    .attr('aria-expanded', true)
                    .removeAttr('aria-controls');

                $expandMeTitle
                    .attr('tabindex', 0);

                $submenu.attr('aria-hidden', false);
            }, 200);
        });
    };

    LessonsSubPageNavigation.prototype.collapse = function($collapseMe) {
        $collapseMe.addClass('sliding-up');
        $collapseMe.find('.lessons-sub-page-menu').slideUp(500, function() {
            var $submenu = $(this);

            $collapseMe.removeClass('sliding-up');
            $collapseMe.removeClass('expanded');

            var $collapseMeIcon = $collapseMe.find('.lessons-expand-collapse-icon')
            var $collapseMeLink = $collapseMe.find('> a');
            var $collapseMeTitle = $collapseMe.find('.lessons-top-level-title');

            $collapseMeLink
                .attr('aria-expanded', false)
                .attr('aria-controls', $collapseMeIcon.attr('aria-controls'));

            $collapseMeIcon
                .attr('title', LESSONS_SUBPAGE_NAVIGATION_LABELS.expand)
                .attr('aria-expanded', false)
                .removeAttr('aria-controls')
                .removeAttr('tabindex')
                .attr('aria-hidden', true);

            $collapseMeTitle
                .removeAttr('tabindex');

            $submenu.attr('aria-hidden', true);
        });
    };


    LessonsSubPageNavigation.prototype.setup_parent_menu = function($li, $menu, submenu_id) {
        var self = this;

        $li.classList.add('has-lessons-sub-pages');
        var $goto = document.createElement('span');

        // stash the top level Lessons page URL
        var topLevelPageHref = $menu.href;
        // force it to be a tool-reset so the session state/breadcrumb
        // is cleared when visiting this top level page
        topLevelPageHref = topLevelPageHref.replace(/\/tool\//, "/tool-reset/");

        $goto.classList.add('lessons-goto-top-page');
        $menu.href = 'javascript:void(0);';

        $menu.setAttribute('aria-controls', submenu_id);
        $menu.setAttribute('aria-expanded', false);

        var $icon = $li.querySelector('.Mrphs-toolsNav__menuitem--link .Mrphs-toolsNav__menuitem--icon');
        $icon.classList.add("lessons-expand-collapse-icon");
        $icon.setAttribute('aria-hidden', true);
        $icon.title = LESSONS_SUBPAGE_NAVIGATION_LABELS.expand;

        var $title = $li.querySelector('.Mrphs-toolsNav__menuitem--link .Mrphs-toolsNav__menuitem--title');
        $title.classList.add("lessons-top-level-title");
        $title.title = LESSONS_SUBPAGE_NAVIGATION_LABELS.open_top_level_page;

        $menu.addEventListener('click', function(event) {
            event.preventDefault();

            // We have jQuery now... YAY, get on that.
            var $li = $PBJQ(event.target).closest('li');

            // when collapsed, a click should take you to the top page and not toggle the menu
            if ($(document.body).is('.Mrphs-toolMenu-collapsed')) {
                location.href = topLevelPageHref;
                return false;
            }

            if ($li.is('.expanded')) {
                // clicked the magic goto span or title span!
                if ($(event.target).is('.lessons-goto-top-page') || $(event.target).is('.lessons-top-level-title')) {
                    location.href = topLevelPageHref;
                    return false;
                }

                // clicked the magic chevron icon!
                if ($(event.target).is(".lessons-expand-collapse-icon")) {
                    event.preventDefault();
  
                    $li.closest('ul').find('.expanded').each(function() {
                        self.collapse($PBJQ(this));
                    });

                    return false;
                }
            } else {
                $li.closest('ul').find('.expanded').each(function() {
                    self.collapse($PBJQ(this));
                });
                self.expand($li);
            }
        });

        $title.addEventListener('keyup', function(event) {
            // We have jQuery now... YAY, get on that.
            var $li = $PBJQ(event.target).closest('li');

            if (event.keyCode == '13') {
                if ($li.is('.expanded')) {

                  // clicked the magic goto span or title span!
                  if ($(event.target).is('.lessons-goto-top-page') || $(event.target).is('.lessons-top-level-title')) {
                      event.preventDefault();
                      event.stopImmediatePropagation();

                      location.href = topLevelPageHref;
                      return false;
                  }
                }
            }

            return true;
        });

        $icon.addEventListener('keyup', function(event) {

            // We have jQuery now... YAY, get on that.
            var $li = $PBJQ(event.target).closest('li');

            if (event.keyCode == '13') {
                if ($li.is('.expanded')) {
                    event.preventDefault();
                    event.stopImmediatePropagation();

                    $li.closest('ul').find('.expanded').each(function() {
                        var $expanded = $PBJQ(this);
                        self.collapse($expanded);
                        $expanded.find('> a').focus()
                    });

                    return false;
                }
            }

            return true;
        });

        if ($li.classList.contains('is-current')) {
            $li.classList.add('expanded');
            var $submenu = $li.querySelector('.lessons-sub-page-menu');
            $submenu.style.display = 'block';
            $submenu.setAttribute('aria-hidden', false);
            $menu.setAttribute('aria-expanded', true);
            $icon.setAttribute('aria-expanded', true);
            $icon.setAttribute('aria-hidden', false);
            $icon.setAttribute('aria-controls', $menu.getAttribute('aria-controls'));
            $menu.removeAttribute('aria-controls');
            $icon.title = LESSONS_SUBPAGE_NAVIGATION_LABELS.collapse;
            $icon.tabIndex = 0;
            $title.tabIndex = 0;
        }

        var $title = $menu.querySelector('.Mrphs-toolsNav__menuitem--title');
        $title.appendChild($goto);
    };


    LessonsSubPageNavigation.prototype.build_sub_page_url_for = function(sub_page) {
        var url = '/portal/site/' + sub_page.siteId;
        url += '/tool/' + sub_page.toolId;
        url += '/ShowPage?sendingPage='+sub_page.sendingPage;
        url += '&itemId='+sub_page.itemId;
        url += '&path=clear_and_push';
        url += '&title=' + sub_page.name;
        url += '&newTopLevel=false';
        return url;
    };

    LessonsSubPageNavigation.prototype.set_current_page_id = function(pageId) {
        if (this.has_current || pageId == this.currentPageId) {
            // if we already have a current set, then there's nothing to do
            return;
        }

        if (this.page_id_to_submenu_item[pageId]) {
            var li = this.page_id_to_submenu_item[pageId];
            // remove is-current from parent item
            var parent = li.parentElement.parentElement;
            parent.classList.add('is-parent-of-current');

            // add is-current to the submenu item
            li.classList.add('is-current');
        }
    };


    window.LessonsSubPageNavigation = LessonsSubPageNavigation;
})();
