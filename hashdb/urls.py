from django.conf.urls import patterns, url

from hashdb import views

urlpatterns = patterns('',
    url(r'^$', views.index, name='index'),
    url(r'^api/window/open$', views.open_window, name='api-open-window'),
	url(r'^api/window/id/([0-9]+)$', views.view_window, name='api-view-window'),
	url(r'^api/hashes$', views.submit_hash, name='api-submit-hash'),
)
