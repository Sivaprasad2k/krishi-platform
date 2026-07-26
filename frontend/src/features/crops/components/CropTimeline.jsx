import React from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar, AlertTriangle, Compass, CheckCircle2 } from 'lucide-react';

export const CropTimeline = ({ crop, incidents = [], treatments = [], tasks = [] }) => {
  const { t } = useTranslation();

  const events = [];

  // 1. Sowing Date (Crop Added)
  if (crop?.plantingDate) {
    events.push({
      date: new Date(crop.plantingDate),
      title: t('crops.timeline.cropAdded', 'Crop Added'),
      description: t('crops.timeline.cropAddedDesc', 'Crop {{name}} ({{variety}}) successfully added.', {
        name: crop.cropName,
        variety: crop.cropVariety
      }),
      icon: Calendar,
      color: 'bg-emerald-500 text-white border-emerald-500'
    });
  }

  // 2. Problems Reported (Disease Incidents)
  incidents.forEach((inc) => {
    if (inc.createdAt) {
      events.push({
        date: new Date(inc.createdAt),
        title: t('crops.timeline.problemReported', 'Problem Reported'),
        description: t('crops.timeline.problemReportedDesc', '{{diseaseName}} reported (Severity: {{severity}})', {
          diseaseName: inc.diseaseName,
          severity: inc.severity
        }),
        icon: AlertTriangle,
        color: 'bg-red-500 text-white border-red-500'
      });
      
      if (inc.status === 'TREATED' || inc.status === 'CLOSED') {
        events.push({
          date: inc.updatedAt ? new Date(inc.updatedAt) : new Date(inc.createdAt),
          title: t('crops.timeline.problemResolved', 'Problem Marked Treated'),
          description: t('crops.timeline.problemResolvedDesc', 'Status: {{status}}', { status: inc.status }),
          icon: CheckCircle2,
          color: 'bg-emerald-500 text-white border-emerald-500'
        });
      }
    }
  });

  // 3. Treatments Applied
  treatments.forEach((treat) => {
    if (treat.createdAt) {
      events.push({
        date: new Date(treat.createdAt),
        title: t('crops.timeline.treatmentApplied', 'Treatment Applied'),
        description: t('crops.timeline.treatmentAppliedDesc', 'Treatment started: {{treatmentName}}', {
          treatmentName: treat.treatmentName
        }),
        icon: Compass,
        color: 'bg-blue-500 text-white border-blue-500'
      });
    }
  });

  // 4. Tasks Completed
  tasks.forEach((task) => {
    if (task.status === 'COMPLETED') {
      events.push({
        date: task.updatedAt ? new Date(task.updatedAt) : new Date(),
        title: t('crops.timeline.taskCompleted', 'Task Completed'),
        description: task.title,
        icon: CheckCircle2,
        color: 'bg-blue-500 text-white border-blue-500'
      });
    }
  });

  events.sort((a, b) => a.date - b.date);

  if (events.length === 0) {
    return (
      <div className="text-center p-4 text-gray-400 font-bold text-sm">
        {t('crops.timeline.noEvents', 'No events logged yet.')}
      </div>
    );
  }

  return (
    <div className="relative pl-6 border-l-2 border-gray-150 space-y-6 ml-2">
      {events.map((evt, idx) => {
        const Icon = evt.icon;
        return (
          <div key={idx} className="relative">
            <span className={`absolute -left-[35px] top-0.5 rounded-full border-2 p-1.5 ${evt.color}`}>
              <Icon className="h-4 w-4 shrink-0" />
            </span>
            
            <div className="space-y-1">
              <span className="text-xs font-black text-gray-400">
                {evt.date.toLocaleDateString()}
              </span>
              <h4 className="text-sm font-black text-gray-800">{evt.title}</h4>
              <p className="text-xs font-semibold text-gray-400">{evt.description}</p>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default CropTimeline;
