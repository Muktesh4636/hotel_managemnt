# Generated manually for TableReservation

import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("venue", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="TableReservation",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("guest_name", models.CharField(max_length=200)),
                ("phone", models.CharField(blank=True, default="", max_length=64)),
                ("party_size", models.PositiveIntegerField(default=2)),
                ("start_epoch_millis", models.BigIntegerField()),
                ("end_epoch_millis", models.BigIntegerField()),
                ("status", models.CharField(default="PENDING", max_length=32)),
                ("notes", models.CharField(blank=True, max_length=2000, null=True)),
                ("created_at_epoch_millis", models.BigIntegerField(default=0)),
                (
                    "owner",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="table_reservations",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
                (
                    "table",
                    models.ForeignKey(
                        blank=True,
                        null=True,
                        on_delete=django.db.models.deletion.SET_NULL,
                        related_name="reservations",
                        to="venue.venuetable",
                    ),
                ),
            ],
            options={
                "ordering": ("start_epoch_millis", "id"),
            },
        ),
    ]
