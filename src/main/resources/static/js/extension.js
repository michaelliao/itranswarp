// extension for markdown content

// init markdown table:
function _mdInitTables() {
	$('#x-content table').each(function () {
		var $t = $(this);
		if (!$t.hasClass('uk-table')) {
			$t.addClass('uk-table');
		}
	});
}

function _get_code(tid) {
    var
        $pre = $('#pre-' + tid),
        $post = $('#post-' + tid),
        $textarea = $('#textarea-' + tid);
    return $pre.text() + $textarea.val() + '\n' + ($post.length === 0 ? '' : $post.text());
}

function run_javascript(tid, btn) {
    var code = _get_code(tid);
    (function () {
        // prepare console.log
        var
            buffer = '',
            _log = function (s) {
                console.log(s);
                buffer = buffer + s + '\n';
            },
            _warn = function (s) {
                console.warn(s);
                buffer = buffer + s + '\n';
            },
            _error = function (s) {
                console.error(s);
                buffer = buffer + s + '\n';
            },
            _console = {
                trace: _log,
                debug: _log,
                log: _log,
                info: _log,
                warn: _warn,
                error: _error
            };
        try {
            eval('(function() {\n var console = _console; \n' + code + '\n})();');
            if (!buffer) {
                buffer = '(no output)';
            }
            showCodeResult(btn, buffer);
        }
        catch (e) {
            buffer = buffer + String(e);
            showCodeError(btn, buffer);
        }
    })();
}

function run_html(tid, btn) {
    var code = _get_code(tid);
    (function () {
        var w = window.open('about:blank', 'Online Practice', 'width=640,height=480,resizeable=1,scrollbars=1');
        w.document.write(code);
        w.document.close();
    })();
}

function _showCodeResult(btn, result, isHtml, isError) {
    var $r = $(btn).next('div.x-code-result');
    if ($r.get(0) === undefined) {
        $(btn).after('<div class="x-code-result x-code uk-alert"></div>');
        $r = $(btn).next('div.x-code-result');
    }
    $r.removeClass('uk-alert-danger');
    if (isError) {
        $r.addClass('uk-alert-danger');
    }
    if (isHtml) {
        $r.html(result);
    } else {
        var ss = result.split('\n');
        var htm = _.map(ss, function (s) {
            return encodeHtml(s).replace(/ /g, '&nbsp;');
        }).join('<br>');
        $r.html(htm);
    }
}

function showCodeResult(btn, result, isHtml) {
    _showCodeResult(btn, result, isHtml);
}

function showCodeError(btn, result, isHtml) {
    _showCodeResult(btn, result, isHtml, true);
}

function run_sql(tid, btn) {
    if (typeof alasql === undefined) {
        showCodeError(btn, '错误：JavaScript嵌入式SQL引擎尚未加载完成，请稍后再试或者刷新页面！');
        return;
    }
    var code = _get_code(tid);
    var genTable = function (arr) {
        if (arr.length === 0) {
            return 'Empty result set';
        }
        var ths = _.keys(arr[0]);
        var trs = _.map(arr, function (obj) {
            return _.map(ths, function (key) {
                return obj[key];
            });
        });
        return '<table class="uk-table"><thead><tr>'
            + $.map(ths, function (th) {
                var n = th.indexOf('!');
                if (n > 1) {
                    th = th.substring(n + 1);
                }
                return '<th>' + encodeHtml(th) + '</th>';
            }).join('') + '</tr></thead><tbody>'
            + $.map(trs, function (tr) {
                return '<tr>' + $.map(tr, function (td) {
                    if (td === undefined) {
                        td = 'NULL';
                    }
                    return '<td>' + encodeHtml(td) + '</td>';
                }).join('') + '</tr>';
            }).join('') + '</tbody></table>';
    };
    (function () {
        var
            i, result, s = '',
            lines = code.split('\n');
        lines = _.map(lines, function (line) {
            var n = line.indexOf('--');
            if (n >= 0) {
                line = line.substring(0, n);
            }
            return line;
        });
        lines = _.filter(lines, function (line) {
            return line.trim() !== '';
        });
        // join:
        for (i = 0; i < lines.length; i++) {
            s = s + lines[i] + '\n';
        }
        // split by ;
        lines = _.filter(s.trim().split(';'), function (line) {
            return line.trim() !== '';
        });
        // run each sql:
        result = null;
        error = null;
        for (i = 0; i < lines.length; i++) {
            s = lines[i];
            try {
                result = alasql(s);
            } catch (e) {
                error = e;
                break;
            }
        }
        if (error) {
            showCodeError(btn, 'ERROR when execute SQL: ' + s + '\n' + String(error));
        } else {
            if (Array.isArray(result)) {
                showCodeResult(btn, genTable(result), true);
            } else {
                showCodeResult(btn, result || '(empty)');
            }
        }
    })();
}

function run_python(tid, btn) {
    var
        code = _get_code(tid),
        $button = $(btn),
        $i = $button.find('i');
    $button.attr('disabled', 'disabled');
    $i.addClass('uk-icon-spinner');
    $i.addClass('uk-icon-spin');
    $.post('https://local.liaoxuefeng.com:39093/run', $.param({
        code: code
    })).done(function (r) {
        showCodeResult(btn, r.output);
    }).fail(function (r) {
        showCodeError(btn, '<p>无法连接到Python代码运行助手。请检查<a target="_blank" href="/wiki/0014316089557264a6b348958f449949df42a6d3a2e542c000/001432523496782e0946b0f454549c0888d05959b99860f000">本机的设置</a>。</p>', true);
    }).always(function () {
        $i.removeClass('uk-icon-spinner');
        $i.removeClass('uk-icon-spin');
        $button.removeAttr('disabled');
    });
}

function run_java(tid, btn) {
    var
        code = _get_code(tid),
        $button = $(btn),
        $i = $button.find('i');
    $button.attr('disabled', 'disabled');
    $i.addClass('uk-icon-spinner');
    $i.addClass('uk-icon-spin');
    $.post('https://local.liaoxuefeng.com:39193/run', $.param({
        code: code
    })).done(function (r) {
        if (r.exitCode === 0) {
            showCodeResult(btn, r.output);
        } else {
            showCodeError(btn, r.output, false);
        }
    }).fail(function (r) {
        showCodeError(btn, '<p>无法连接到Java代码运行助手。请检查<a target="_blank" href="/wiki/001543970808338ad98bbeaa6fc405c8df49d6a015b6e67000/001543970112198a66c30326d4c4ba38684767edcc16912000">本机的设置</a>。</p>', true);
    }).always(function () {
        $i.removeClass('uk-icon-spinner');
        $i.removeClass('uk-icon-spin');
        $button.removeAttr('disabled');
    });
}

function adjustTextareaHeight(t) {
    var
        $t = $(t),
        lines = $t.val().split('\n').length;
    if (lines < 9) {
        lines = 9;
    }
    $t.attr('rows', '' + (lines + 1));
}

var initRunCode = (function() {
    var tid = 0;
    var trimCode = function (code) {
        var ch;
        while (code.length > 0) {
            ch = code[0];
            if (ch === '\n' || ch === '\r') {
                code = code.substring(1);
            }
            else {
                break;
            }
        }
        while (code.length > 0) {
            ch = code[code.length - 1];
            if (ch === '\n' || ch === '\r') {
                code = code.substring(0, code.length - 1);
            }
            else {
                break;
            }
        }
        return code + '\n';
    };
    var initPre = function ($pre, fn_run) {
        tid++;
        var
            theId = 'online-run-code-' + tid,
            $code = $pre.children('code'),
            $post = null,
            codes = $code.text().split('----', 3);
        $code.remove();
        $pre.attr('id', 'pre-' + theId);
        $pre.css('font-size', '14px');
        $pre.css('margin-bottom', '0');
        $pre.css('border-bottom', 'none');
        $pre.css('padding', '6px');
        $pre.css('border-bottom-left-radius', '0');
        $pre.css('border-bottom-right-radius', '0');
        $pre.wrap('<form class="uk-form uk-form-stack uk-margin-top uk-margin-bottom" action="#0"></form>');
        $pre.after('<button type="button" onclick="' + fn_run + '(\'' + theId + '\', this)" class="uk-button uk-button-primary" style="margin-top:15px;"><i class="uk-icon-play"></i> Run</button>');
        if (codes.length === 1) {
            codes.unshift('');
            codes.push('');
        } else if (codes.length === 2) {
            codes.push('');
        }
        $pre.text(trimCode(codes[0]))
        if (codes[2].trim()) {
            // add post:
            $pre.after('<pre id="post-' + theId + '" style="font-size: 14px; margin-top: 0; border-top: 0; padding: 6px; border-top-left-radius: 0; border-top-right-radius: 0;"></pre>');
            $post = $('#post-' + theId);
            $post.text(trimCode(codes[2]));
        }
        $pre.after('<textarea id="textarea-' + theId + '" onkeyup="adjustTextareaHeight(this)" class="uk-width-1-1 x-codearea" rows="10" style="overflow: scroll; border-top-left-radius: 0; border-top-right-radius: 0;' + ($post === null ? '' : 'border-bottom-left-radius: 0; border-bottom-right-radius: 0;') + '"></textarea>');
        $('#textarea-' + theId).val(trimCode(codes[1]));
        adjustTextareaHeight($('#textarea-' + theId).get(0));
    };
    return initPre;
})();

function checkChoice(formId) {
    var
        $form = $('#' + formId),
        $yes = $form.find('span.uk-text-success'),
        $no = $form.find('span.uk-text-danger'),
        $checkboxes = $form.find('input[type=checkbox]');
    $yes.show();
    $no.hide();
    $checkboxes.each(function (i, c) {
        var
            $c = $(c),
            shouldCheck = $c.attr('x-data') === 'x',
            isCheck = $c.is(':checked');
        if (shouldCheck !== isCheck) {
            $yes.hide();
            $no.show();
        }
    });
}

var choiceId = 0;

function initChoice($pre) {
    choiceId++;
    console.log('init choice ' + choiceId);
    var
        i, x, c,
        id = 'form-choice-' + choiceId,
        codes = $pre.children('code').text().split('----', 2),
        question = codes[0],
        choices = $.trim(codes[1]).split('\n');
    var h = '<form id="' + id + '" class="uk-form uk-margin-top uk-margin-bottom"><fieldset><legend><i class="uk-icon-question-circle"></i> ' + encodeHtml(question) + '</legend>';
    for (i = 0; i < choices.length; i++) {
        c = $.trim(choices[i]);
        x = c.indexOf('[x]') === 0 || c.indexOf('(x)') === 0;
        if (x) {
            c = c.substring(3);
        }
        h = h + '<div class="uk-form-row"><label><input type="checkbox" ' + (x ? 'x-data="x"' : '') + '> ' + encodeHtml(c) + '</label></div>';
    }
    h = h + '<div class="uk-form-row"><button type="button" class="uk-button uk-button-primary" onclick="checkChoice(\'' + id + '\')">Submit</button>&nbsp;&nbsp;&nbsp;';
    h = h + '<span class="uk-text-large uk-text-success" style="display:none"><i class="uk-icon-check"></i></span>';
    h = h + '<span class="uk-text-large uk-text-danger" style="display:none"><i class="uk-icon-times"></i></span></div></fieldset></form>';
    $pre.replaceWith(h);
}

function _mdInitRunCode() {
    $('pre>code').each(function (i, code) {
        var
            $code = $(code),
            classes = ($code.attr('class') || '').split(' '),
            nohightlight = (_.find(classes, function (s) { return s.indexOf('language-nohightlight') >= 0; }) || '').trim(),
            warn = (_.find(classes, function (s) { return s.indexOf('language-!') >= 0; }) || '').trim(),
            info = (_.find(classes, function (s) { return s.indexOf('language-?') >= 0; }) || '').trim(),
            choice = (_.find(classes, function (s) { return s.indexOf('language-choice') >= 0; }) || '').trim(),
            x_run = (_.find(classes, function (s) { return s.indexOf('language-x-') >= 0; }) || '').trim();
        console.log("init " + $code);
        if ($code.hasClass('language-ascii')) {
            // set ascii style for markdown:
            $code.css('font-family', '"Courier New",Consolas,monospace')
                .parent('pre')
                .css('font-size', '12px')
                .css('line-height', '12px')
                .css('border', 'none')
                .css('white-space', 'pre')
                .css('background-color', 'transparent');
        } else if (choice) {
            initChoice($code.parent());
        } else if (warn || info) {
            $code.parent().replaceWith('<div class="uk-alert ' + (warn ? 'uk-alert-danger' : '') + '"><i class="uk-icon-' + (warn ? 'warning' : 'info-circle') + '"></i> ' + encodeHtml($code.text()) + '</div>');
        } else if (x_run) {
            var fn = 'run_' + x_run.substring('language-x-'.length);
            initRunCode($code.parent(), fn);
        } else if (!nohightlight) {
            hljs.highlightBlock(code);
        }
    });
}

$(function () {
	var tryFn = function (fn) {
		try {
			fn();
		} catch (err) {
			console.log(err);
		}
	};
	tryFn(_mdInitTables);
	tryFn(_mdInitRunCode);
	console.log('init extensions ok.');
});
