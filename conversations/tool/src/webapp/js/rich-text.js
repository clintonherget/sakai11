(function() {

    var initialize = function (opts) {
        if ($(opts.elt).hasClass('rich-text-initialized')) {
            return;
        }

        InlineEditor
            .create(opts.elt, {
                placeholder: (opts.placeholder || "Type something")
            })
            .then(newEditor => {
                opts.onCreate(newEditor);
                this.editor.ui.focusTracker.on('change:isFocused', (event, name, isFocused) => {
                    opts.onFocus(event, name, isFocused);
                });

            })
            .catch(function (error) {
                console.error(error);
            });

        $(opts.elt).addClass('rich-text-initialized');
    };

    window.RichText = {}
    window.RichText.initialize = initialize;
}());
