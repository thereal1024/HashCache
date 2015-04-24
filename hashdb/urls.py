from django.conf.urls import patterns, url
from hashdb import views

urlpatterns = patterns('',
    #url(r'^$', views.index, name='index'),
    url(r'^api/window/open$', views.open_window, name='api-open-window'),
    url(r'^api/window/id/([0-9]+)$', views.view_window, name='api-view-window'),
    url(r'^api/hashes$', views.submit_hash, name='api-submit-hash'),
    url(r'^api/hashes/([0-9a-fA-F]{64})$', views.hash_info, name='api-hash-info'),
    url(r'^api/tree/([0-9a-fA-F]{64})$', views.proof_tree, name='api-proof-tree'),
    url(r'^api/recent/$', views.recent_hashes, name='api-recent-hashes')
)

urlpatterns += patterns(
    'django.contrib.staticfiles.views',
    url(r'^(?:index.html)?$', 'serve', kwargs={'path': 'index.html'}),
    url(r'^(?P<path>(?:js|css|img)/.*)$', 'serve'),
)
