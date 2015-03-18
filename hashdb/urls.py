from django.conf.urls import patterns, url

from hashdb import views

urlpatterns = patterns('',
    url(r'^$', views.index, name='index'),
)
