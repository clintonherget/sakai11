(function() {

    class UploadAdapter {
        constructor(loader, baseurl) {
            this.loader = loader;
            this.baseurl = baseurl;
        }

        upload() {
            return this.loader.file.then(file =>
                new Promise((resolve, reject) => {
                    this.handleUpload(file, resolve, reject);
                }));
        }

        abort() {
        }

        handleUpload(file, resolve, reject) {
            var self = this;
            var formData = new FormData();
            formData.append("file", file);
            formData.append("mode", "inline-upload");

            $.ajax({
                url: self.baseurl + "file-upload",
                type: "POST",
                contentType: false,
                cache: false,
                processData: false,
                data: formData,
                dataType: 'json',
                success: function (response) {
                    resolve({
                        default: self.baseurl + "file-view?mode=view&key=" + response.key
                    });
                },
                error: function (xhr, statusText) {
                    reject(statusText);
                }
            });
        }
    }


    var initialize = function (opts) {
        if ($(opts.elt).hasClass('rich-text-initialized')) {
            return;
        }

        InlineEditor
            .create(opts.elt, {
                placeholder: (opts.placeholder || "Type something")
            })
            .then(newEditor => {
                newEditor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
                    return new UploadAdapter(loader, opts.baseurl);
                };

                newEditor.ui.focusTracker.on('change:isFocused', (event, name, isFocused) => {
                    opts.onFocus(event, name, isFocused);
                });

                opts.onCreate(newEditor);
            })
            .catch(function (error) {
                console.error(error);
            });

        $(opts.elt).addClass('rich-text-initialized');
    };

    window.RichText = {}
    window.RichText.initialize = initialize;
}());
