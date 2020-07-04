from django.db import models
from django.utils import timezone
# Create your models here.

class AbstractModel(models.Model):
    create_date = models.DateTimeField(
        db_column="create_date",
        verbose_name="create Date",
        help_text="Date on which the record was inserted. This is by default values and will be update using python function save.",
        editable=False,
        blank=True,
        auto_now_add=True
    )
    update_date = models.DateTimeField(
        db_column="update_date",
        verbose_name="update Date",
        help_text="Date on which the record was update. This is by default values and will be update using python function save.",
        blank=True,
        editable=False,
        auto_now_add=True
    )
    is_active = models.BooleanField(
        db_column="is_active",
        default=True,
        verbose_name="Is Active",
        help_text="This column is used for soft delete. Users can reactivate the entry via portal",
    )

    def save(self, *args, **kwargs):
        """ On save, update timestamps """
        if not self.id:
            self.create_date = timezone.now()
        self.update_date = timezone.now()
        super().save(*args, **kwargs)

    class Meta:
       abstract = True


class UserMaster(AbstractModel):
    user_name = models.CharField(
        db_column="user_name",
        max_length = 255,
        verbose_name="User Name",
        help_text="Name of different Users",
    )
    address = models.CharField(
        db_column="address",
        max_length = 50,
        verbose_name="Address",
        help_text="Addrss of the User",
    )

    contact_info = models.CharField(
        db_column="contact_info",
        max_length = 50,

        verbose_name="Contact Info",
        help_text="Contact info of User",
    )

    class Meta:
        db_table = "user_master"
        verbose_name = "User Master"
        verbose_name_plural = "User Masters"

    def __str__(self):
        return f"{self.user_name}"

class ActivityMaster(AbstractModel):
    user = models.ForeignKey(
        UserMaster,
        on_delete=models.CASCADE,
        db_column="user",
        verbose_name="USER   ",
        help_text="User relation",
    )
    start_time = models.DateTimeField(
        db_column="start_time",
        verbose_name="Start Time",
        help_text="Time when user logged in",

    )
    end_time = models.DateTimeField(
        db_column="end_time",
        verbose_name="End Time",
        help_text="Use logged out time",
        
    )

    class Meta:
        db_table = "activity_master"
        verbose_name = "Activity Master"
        verbose_name_plural = "Activity Masters"

    def __str__(self):
        return f"{self.user}"


