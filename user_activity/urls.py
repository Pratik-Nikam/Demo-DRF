from django.conf.urls import url
from .views import GenericView
from .models import UserMaster,ActivityMaster
from .serializers import UserMasterSerializer,ActivityMasterSerializer



urlpatterns = [
    url('activity-master/', GenericView.as_view(),{'model': ActivityMaster, 'serializer_class' : ActivityMasterSerializer}),   
]