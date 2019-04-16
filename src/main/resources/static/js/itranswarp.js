// itranswarp.js

$(function () {
    // activate navigation menu:
    var xnav = $('meta[property="x-nav"]').attr('content');
    xnav && xnav.trim() && $('#ul-navbar li a[href="' + xnav.trim() + '"]').parent().addClass('uk-active');

    // init scroll:
    var $window = $(window);
    var $body = $('body');
    var $gotoTop = $('div.x-goto-top');
    // lazy load:
    var lazyImgs = _.map($('img[data-src]').get(), function (i) {
        return $(i);
    });
    var onScroll = function () {
        var wtop = $window.scrollTop();
        if (wtop > 1200) {
            $gotoTop.show();
        }
        else {
            $gotoTop.hide();
        }
        if (lazyImgs.length > 0) {
            var wheight = $window.height();
            var loadedIndex = [];
            _.each(lazyImgs, function ($i, index) {
                if ($i.offset().top - wtop < wheight) {
                    $i.attr('src', $i.attr('data-src'));
                    loadedIndex.unshift(index);
                }
            });
            _.each(loadedIndex, function (index) {
                lazyImgs.splice(index, 1);
            });
        }
    };
    $window.scroll(onScroll);
    onScroll();

    // go-top:
    $gotoTop.click(function () {
        $('html, body').animate({ scrollTop: 0 }, 1000);
    });

    // on resize:
    var autoResizeNavBar = function () {
	    var
	        $navbar = $('#navbar'),
	        $brand = $('#brand'),
	        $brand2 = $('#brand-small'),
	        $ul = $('#ul-navbar'),
	        $ulList = [],
	        $more = $('#navbar-more'),
	        $moreList = [],
	        $user = $('#navbar-user-info'),
	        minNavWidth = 0;
	    $ul.find('>li.x-nav').each(function () {
	        minNavWidth += $(this).outerWidth();
	        $ulList.push($(this));
	    });
	    $('#ul-navbar-more').find('>li.x-nav').each(function () {
	        $moreList.push($(this));
	    });
        var total = $navbar.width() - 6;
        if ($brand.is(':visible')) {
            total -= $brand.outerWidth();
        }
        if ($brand2.is(':visible')) {
            total -= $brand2.outerWidth();
        }
        total -= $user.outerWidth();
        if (total >= minNavWidth) {
            $more.hide();
            $.each($ulList, function (index, nav) {
                nav.show();
            });
        } else {
            $more.show();
            var
                i,
                skip = false,
                actualW = 0,
                maxW = total - $more.outerWidth();
            for (i = 0; i < $ulList.length; i++) {
                var
                    $t = $ulList[i],
                    $m = $moreList[i],
                    w = $t.outerWidth();
                if (!skip && (actualW + w > maxW)) {
                    skip = true;
                } else {
                    actualW += w;
                }
                if (skip) {
                    $t.hide();
                    $m.show();
                } else {
                    $t.show();
                    $m.hide();
                }
            }
        }
    };
    $window.resize(autoResizeNavBar);
});

function deleteTopic(topicId) {
	UIkit.modal.confirm("Are you sure to delete this topic?", function(){
		postJSON('/api/topics/' + topicId + '/delete', {}, function (err, resp) {
			if (err) {
				return showError(err);
			}
			refresh();
		});
	});
}

function deleteTopicAndLockUser(topicId, userId){
	UIkit.modal.confirm("Are you sure to delete this topic and lock user?", function(){
		postJSON('/api/topics/' + topicId + '/delete', {}, function (err, resp) {
			if (err) {
				return showError(err);
			}
			_lockUser(userId);
		});
	});
}

function deleteReply(replyId) {
	UIkit.modal.confirm("Are you sure to delete this reply?", function(){
		postJSON('/api/replies/' + replyId + '/delete', {}, function (err, resp) {
			if (err) {
				return showError(err);
			}
			refresh();
		});
	});
}

function deleteReplyAndLockUser(replyId, userId){
	UIkit.modal.confirm("Are you sure to delete this reply and lock user?", function(){
		postJSON('/api/replies/' + replyId + '/delete', {}, function (err, resp) {
			if (err) {
				return showError(err);
			}
			_lockUser(userId);
		});
	});
}

function _lockUser(userId) {
	postJSON('/api/users/' + userId + '/lock/5000000000000', {}, function (err, resp) {
		if (err) {
			return showError(err);
		}
		refresh();
	});
}

// comment ////////////////////////////////////////////////////////////////////

function ajaxLoadComments(insertIntoId, ref_id) {
    var errorHtml = 'Error when loading. <a href="#0" onclick="ajaxLoadComments(\'' + insertIntoId + '\', \'' + ref_id + '\')">Retry</a>';
    $insertInto = $('#' + insertIntoId);
    $insertInto.html('<i class="uk-icon-spinner uk-icon-spin"></i> Loading...');
    $.getJSON('/api/ref/' + ref_id + '/topics').done(function (data) {
        if (data.error) {
            $insertInto.html(errorHtml);
            return;
        }
        // build comment list:
        $insertInto.html(buildComments(data.results));
        $insertInto.find('.x-auto-content').each(function () {
            makeCollapsable(this, 400);
        });
    }).fail(function () {
        $insertInto.html(errorHtml);
    });
}

function initCommentArea(ref_type, ref_id, tag) {
    $('#x-comment-area').html($('#tplCommentArea').html());
    var $makeComment = $('#comment-make-button');
    var $commentForm = $('#comment-form');
    var $postComment = $commentForm.find('button[type=submit]');
    var $cancelComment = $commentForm.find('button.x-cancel');
    $makeComment.click(function () {
        $commentForm.showFormError();
        $commentForm.show();
        $commentForm.find('div.x-textarea').html('<textarea></textarea>');
        var editor = new Simditor({
			textarea: $commentForm.find('textarea'),
			toolbarFloatOffset: 50,
			toolbar: ['title', '|', 'bold', 'italic', 'strikethrough', '|', 'blockquote', 'code', 'link', '|', 'ol', 'ul', '|', 'hr']
		});
        $makeComment.hide();
    });
    $cancelComment.click(function () {
        $commentForm.find('div.x-textarea').html('');
        $commentForm.hide();
        $makeComment.show();
    });
    $commentForm.submit(function (e) {
        e.preventDefault();
        $commentForm.postJSON('/api/comments/' + tag, {
            refType: ref_type.toUpperCase(),
            refId: ref_id,
            name: $commentForm.find('input[name=name]').val(),
            content: html2md($commentForm.find('textarea').val())
        }, function (err, result) {
            if (err) {
                return;
            }
            refresh('#comments');
        });
    });
}

function loadComments(ref_id) {
    $(function () {
        var
            isCommentsLoaded = false,
            $window = $(window),
            targetOffset = $('#x-comment-list').get(0).offsetTop,
            checkOffset = function () {
                if (!isCommentsLoaded && (window.pageYOffset + window.innerHeight >= targetOffset)) {
                    isCommentsLoaded = true;
                    $window.off('scroll', checkOffset);
                    ajaxLoadComments('x-comment-list', ref_id);
                }
            };
        $window.scroll(checkOffset);
        checkOffset();
    });
}

// load topics as comments:

var
    tplComment = null,
    tplCommentReply = null,
    tplCommentInfo = null;

function buildComments(topics) {
    if (tplComment === null) {
        tplComment = new Template($('#tplComment').html());
    }
    if (tplCommentReply === null) {
        tplCommentReply = new Template($('#tplCommentReply').html());
    }
    if (tplCommentInfo === null) {
        tplCommentInfo = new Template($('#tplCommentInfo').html());
    }
    if (topics.length === 0) {
        return '<p>No comment yet.</p>';
    }
	L = [];
    topics.map(function (topic) {
        L.push('<li>');
        L.push(tplComment.render(topic));
        L.push('<ul>')
    	topic.replies.map(function (reply) {
            L.push('<li>');
            L.push(tplCommentReply.render(reply));
            L.push('</li>');
    	});
        L.push(tplCommentInfo.render(topic));
        L.push('</ul>');
        L.push('</li>');
    });
    return L.join('');
}

function makeCollapsable(obj, max_height) {
    var $o = $(obj);
    if ($o.height() <= (max_height + 60)) {
        $o.show();
        return;
    }
    var maxHeight = max_height + 'px';
    $o.css('max-height', maxHeight);
    $o.css('overflow', 'hidden');
    $o.after('<p style="padding-left: 75px">' +
        '<a href="#0"><i class="uk-icon-chevron-down"></i> Read More</a>' +
        '<a href="#0" style="display:none"><i class="uk-icon-chevron-up"></i> Collapse</a>' +
        '</p>');
    var aName = 'COLLAPSE-' + nextId();
    $o.parent().before('<div class="x-anchor"><a name="' + aName + '"></a></div>')
    var $p = $o.next();
    var $aDown = $p.find('a:first');
    var $aUp = $p.find('a:last');
    $aDown.click(function () {
        $o.css('max-height', 'none');
        $aDown.hide();
        $aUp.show();
    });
    $aUp.click(function () {
        $o.css('max-height', maxHeight);
        $aUp.hide();
        $aDown.show();
        location.assign('#' + aName);
    });
    $o.show();
}

$(function () {
    $('.x-auto-content').each(function () {
        makeCollapsable(this, 300);
    });
});

// markdown ///////////////////////////////////////////////////////////////////

function html2md(s) {
	return new TurndownService({
		headingStyle: 'atx',
		codeBlockStyle: 'fenced',
		emDelimiter: '*',
		hr: '----'
	}).turndown(s);
}

// begin ad ///////////////////////////////////////////////////////////////////

function _adFilterMaterials(metaTags, materials) {
	if (materials.length === 0) {
		return [];
	}
	var matched = materials.filter(function (material) {
		var filteredTags = metaTags.filter(function (tag) {
			return material.tags.indexOf(tag) >= 0;
		});
		return filteredTags.length > 0;
	});
	return matched.length > 0 ? matched : materials;
}

var _adImagePlaceHolder = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='128' height='128' x='0px' y='0px' viewBox='0 0 469.333 469.333'%3E%3Cg%3E%3Cpath d='M426.667,0h-384C19.135,0,0,19.135,0,42.667v384c0,23.531,19.135,42.667,42.667,42.667h384 c23.531,0,42.667-19.135,42.667-42.667v-384C469.333,19.135,450.198,0,426.667,0z M448,426.667 c0,11.76-9.573,21.333-21.333,21.333h-384c-11.76,0-21.333-9.573-21.333-21.333v-320H448V426.667z M448,85.333H21.333V42.667 c0-11.76,9.573-21.333,21.333-21.333h384c11.76,0,21.333,9.573,21.333,21.333V85.333z'/%3E%3Ccircle cx='53.333' cy='53.333' r='10.667'/%3E%3Ccircle cx='96' cy='53.333' r='10.667'/%3E%3Ccircle cx='138.667' cy='53.333' r='10.667'/%3E%3Cpath d='M74.667,341.333H288c5.896,0,10.667-4.771,10.667-10.667V160c0-5.896-4.771-10.667-10.667-10.667H74.667 C68.771,149.333,64,154.104,64,160v170.667C64,336.563,68.771,341.333,74.667,341.333z M85.333,170.667h192V320h-192V170.667z'/%3E%3Cpath d='M138.667,192c-17.646,0-32,14.354-32,32v64c0,5.896,4.771,10.667,10.667,10.667c5.896,0,10.667-4.771,10.667-10.667 v-10.667h21.333V288c0,5.896,4.771,10.667,10.667,10.667c5.896,0,10.667-4.771,10.667-10.667v-64 C170.667,206.354,156.312,192,138.667,192z M149.333,256H128v-32c0-5.885,4.781-10.667,10.667-10.667s10.667,4.781,10.667,10.667 V256z'/%3E%3Cpath d='M224,192h-21.333c-5.896,0-10.667,4.771-10.667,10.667V288c0,5.896,4.771,10.667,10.667,10.667H224 c17.646,0,32-14.354,32-32V224C256,206.354,241.646,192,224,192z M234.667,266.667c0,5.885-4.781,10.667-10.667,10.667h-10.667 v-64H224c5.885,0,10.667,4.781,10.667,10.667V266.667z'/%3E%3Cpath d='M74.667,405.333h320c5.896,0,10.667-4.771,10.667-10.667c0-5.896-4.771-10.667-10.667-10.667h-320 C68.771,384,64,388.771,64,394.667C64,400.563,68.771,405.333,74.667,405.333z'/%3E%3Cpath d='M330.667,213.333h64c5.896,0,10.667-4.771,10.667-10.667S400.563,192,394.667,192h-64 c-5.896,0-10.667,4.771-10.667,10.667S324.771,213.333,330.667,213.333z'/%3E%3Cpath d='M330.667,277.333h64c5.896,0,10.667-4.771,10.667-10.667S400.563,256,394.667,256h-64 c-5.896,0-10.667,4.771-10.667,10.667S324.771,277.333,330.667,277.333z'/%3E%3Cpath d='M330.667,341.333h64c5.896,0,10.667-4.771,10.667-10.667c0-5.896-4.771-10.667-10.667-10.667h-64 c-5.896,0-10.667,4.771-10.667,10.667C320,336.563,324.771,341.333,330.667,341.333z'/%3E%3C/g%3E%3C/svg%3E";
var _adTemplate = '<div style="float:left;overflow:hidden;box-sizing:border-box;margin:0 0 -1px -1px;border:solid 1px #ddd;width:${width}px;height:${height}px;"><a target="_blank" href="${url}"><img style="width:${width}px;height:${height}px;" src="/files/attachments/${imageId}/0"></a></div>';
var _adDefault  = '<div style="float:left;overflow:hidden;box-sizing:border-box;margin:0 0 -1px -1px;border:solid 1px #ddd;width:${width}px;height:${height}px;"><a target="_blank" href="${url}"><img style="width:100px;height:100px" src="' + _adImagePlaceHolder + '"></a></div>';
var _adAutoFill = '<div style="float:left;overflow:hidden;box-sizing:border-box;margin:0 0 -1px -1px;border:solid 1px #ddd;width:${width}px;height:${height}px;">${content}</div>';

function _adAddSponsor(slot, template, material) {
	var s = template.replace(/\$\{width\}/g, '' + slot.width)
			.replace(/\$\{height\}/g, '' + slot.height)
			.replace(/\$\{imageId\}/g, '' + material.imageId)
			.replace(/\$\{url\}/g, material.url)
			.replace(/\$\{content\}/g, material.content);
	$('#sponsor-' + slot.alias).append(s);
}

function _adRandomMaterial(materials) {
	if (materials.length === 0) {
		return null;
	}
	if (materials.length === 1) {
		return materials[0];
	}
	var
		weights = materials.map(function (m) { return m.weight; }),
		total_weights = weights.reduce(function (ax, w) { return ax + w; }, 0),
		rnd = Math.random(),
		ws = 0,
		i,
		hit;
	for (i=0; i < weights.length; i++) {
		ws = ws + weights[i];
		if (rnd < ws / total_weights) {
			return materials[i];
		}
	}
	return materials[0];
}

function initSponsors(ads) {
	// ad enabled?
	var meta_ad = $('meta[property="og:ad"]').attr('content');
	if (meta_ad !== 'true') {
		return;
	}

	// try get tags as string array like ['abc', 'xyz']:
	var
		meta_tag = $('meta[property="og:tag"]').attr('content') || '',
		meta_tags = meta_tag.toLowerCase().split(',');

	// process each slot:
	ads.slots.forEach(function (slot) {
		$('#sponsor-' + slot.alias).addClass('x-sponsor');
		slot.periods.forEach(function (period) {
			var
				materials = _adFilterMaterials(meta_tags, period.materials),
				material = _adRandomMaterial(materials);
			if (material) {
				_adAddSponsor(slot, _adTemplate, material);
			} else {
				_adAddSponsor(slot, _adDefault, { imageId: 0, url: '/' });
			}
		});
		for (var i=0; i < Math.min(slot.numAutoFill, slot.numSlots - slot.periods.length); i++) {
			_adAddSponsor(slot, _adAutoFill, { content: slot.adAutoFill });
		}
	});
}

// end ad /////////////////////////////////////////////////////////////////////
