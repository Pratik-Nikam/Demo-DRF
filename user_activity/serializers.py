from rest_framework import serializers
from .models import UserMaster,ActivityMaster

class UserMasterSerializer(serializers.ModelSerializer):    
    class Meta:
        model = UserMaster
        fields = ['user_name', 'address' ,'contact_info']

class ActivityMasterSerializer(serializers.ModelSerializer):    
    user = UserMasterSerializer( read_only=True, required=False)
    class Meta:
        model = ActivityMaster
        fields = ['user','start_time', 'end_time']
        depth = 1
