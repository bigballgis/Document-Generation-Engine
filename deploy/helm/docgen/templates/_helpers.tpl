{{/*
Expand the name of the chart.
*/}}
{{- define "docgen.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "docgen.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Backend workload full name.
*/}}
{{- define "docgen.backend.fullname" -}}
{{- printf "%s-backend" (include "docgen.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Frontend workload full name.
*/}}
{{- define "docgen.frontend.fullname" -}}
{{- printf "%s-frontend" (include "docgen.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to all resources.
*/}}
{{- define "docgen.labels" -}}
helm.sh/chart: {{ include "docgen.chart" . }}
{{ include "docgen.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: docgen
{{- end }}

{{/*
Chart label helper.
*/}}
{{- define "docgen.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Selector labels shared by Deployment and Service.
*/}}
{{- define "docgen.selectorLabels" -}}
app.kubernetes.io/name: {{ include "docgen.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Inactive blue-green color (blue <-> green).
*/}}
{{- define "docgen.blueGreen.inactiveColor" -}}
{{- if eq .Values.blueGreen.activeColor "blue" -}}green{{- else -}}blue{{- end -}}
{{- end }}

{{/*
Backend selector labels.
*/}}
{{- define "docgen.backend.selectorLabels" -}}
{{ include "docgen.selectorLabels" . }}
app.kubernetes.io/component: backend
{{- end }}

{{/*
Backend selector labels for a blue-green color Deployment.
*/}}
{{- define "docgen.backend.color.selectorLabels" -}}
{{ include "docgen.backend.selectorLabels" .root }}
docgen.io/deployment-color: {{ .color }}
{{- end }}

{{/*
Backend Deployment name for a blue-green color.
*/}}
{{- define "docgen.backend.color.fullname" -}}
{{- printf "%s-%s" (include "docgen.backend.fullname" .root) .color | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Backend image for a blue-green color (color tag overrides global backend.image.tag).
*/}}
{{- define "docgen.backend.color.image" -}}
{{- $colorTag := index .root.Values.blueGreen.colors .color "backendImageTag" -}}
{{- $tag := $colorTag | default .root.Values.backend.image.tag | default .root.Chart.AppVersion -}}
{{- printf "%s:%s" .root.Values.backend.image.repository $tag }}
{{- end }}

{{/*
Frontend selector labels.
*/}}
{{- define "docgen.frontend.selectorLabels" -}}
{{ include "docgen.selectorLabels" . }}
app.kubernetes.io/component: frontend
{{- end }}

{{/*
Frontend selector labels for a blue-green color Deployment.
*/}}
{{- define "docgen.frontend.color.selectorLabels" -}}
{{ include "docgen.frontend.selectorLabels" .root }}
docgen.io/deployment-color: {{ .color }}
{{- end }}

{{/*
Frontend Deployment name for a blue-green color.
*/}}
{{- define "docgen.frontend.color.fullname" -}}
{{- printf "%s-%s" (include "docgen.frontend.fullname" .root) .color | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Frontend image for a blue-green color.
*/}}
{{- define "docgen.frontend.color.image" -}}
{{- $colorTag := index .root.Values.blueGreen.colors .color "frontendImageTag" -}}
{{- $tag := $colorTag | default .root.Values.frontend.image.tag | default .root.Chart.AppVersion -}}
{{- printf "%s:%s" .root.Values.frontend.image.repository $tag }}
{{- end }}

{{/*
ConfigMap name for non-sensitive application configuration.
*/}}
{{- define "docgen.configMapName" -}}
{{- printf "%s-config" (include "docgen.fullname" .) }}
{{- end }}

{{/*
Secret name for credentials (existing or chart-managed).
Fail-closed: existingSecretName is required when secrets.create is false.
*/}}
{{- define "docgen.secretName" -}}
{{- if .Values.secrets.create }}
{{- printf "%s-secrets" (include "docgen.fullname" .) }}
{{- else if .Values.secrets.externalSecret.enabled }}
{{- printf "%s-secrets" (include "docgen.fullname" .) }}
{{- else }}
{{- required "secrets.existingSecretName is required when secrets.create is false (fail-closed — no silent insecure default)" .Values.secrets.existingSecretName }}
{{- end }}
{{- end }}

{{/*
In-cluster Kubernetes DNS FQDN for backend Service (P15-T04a).
Pattern: {release}-backend.{namespace}.svc.cluster.local
*/}}
{{- define "docgen.backend.serviceFqdn" -}}
{{- printf "%s.%s.svc.cluster.local" (include "docgen.backend.fullname" .) .Release.Namespace }}
{{- end }}

{{/*
In-cluster Kubernetes DNS FQDN for frontend Service (P15-T04a).
Pattern: {release}-frontend.{namespace}.svc.cluster.local
*/}}
{{- define "docgen.frontend.serviceFqdn" -}}
{{- printf "%s.%s.svc.cluster.local" (include "docgen.frontend.fullname" .) .Release.Namespace }}
{{- end }}

{{/*
TLS secret name for Ingress / Certificate.
*/}}
{{- define "docgen.tlsSecretName" -}}
{{- if .Values.ingress.tls.secretName }}
{{- .Values.ingress.tls.secretName }}
{{- else if .Values.certificate.secretName }}
{{- .Values.certificate.secretName }}
{{- else }}
{{- printf "%s-tls" (include "docgen.fullname" .) }}
{{- end }}
{{- end }}

{{/*
Certificate DNS names (defaults to ingress host).
*/}}
{{- define "docgen.certificateDnsNames" -}}
{{- if .Values.certificate.dnsNames }}
{{- toYaml .Values.certificate.dnsNames }}
{{- else }}
{{- .Values.ingress.host | quote }}
{{- end }}
{{- end }}

{{/*
Backend replica count — HPA minReplicas when autoscaling is enabled.
*/}}
{{- define "docgen.backend.replicas" -}}
{{- if .Values.autoscaling.backend.enabled }}
{{- .Values.autoscaling.backend.minReplicas }}
{{- else }}
{{- .Values.backend.replicaCount }}
{{- end }}
{{- end }}

{{/*
Frontend replica count — HPA minReplicas when autoscaling is enabled.
*/}}
{{- define "docgen.frontend.replicas" -}}
{{- if .Values.autoscaling.frontend.enabled }}
{{- .Values.autoscaling.frontend.minReplicas }}
{{- else }}
{{- .Values.frontend.replicaCount }}
{{- end }}
{{- end }}

{{/*
Frontend NGINX ConfigMap name.
*/}}
{{- define "docgen.frontend.nginxConfigMapName" -}}
{{- printf "%s-frontend-nginx" (include "docgen.fullname" .) }}
{{- end }}

{{/*
Resolve backend image reference.
*/}}
{{- define "docgen.backend.image" -}}
{{- $tag := .Values.backend.image.tag | default .Chart.AppVersion }}
{{- printf "%s:%s" .Values.backend.image.repository $tag }}
{{- end }}

{{/*
Resolve frontend image reference.
*/}}
{{- define "docgen.frontend.image" -}}
{{- $tag := .Values.frontend.image.tag | default .Chart.AppVersion }}
{{- printf "%s:%s" .Values.frontend.image.repository $tag }}
{{- end }}

{{/*
Pod-level securityContext baseline (ADR-0030 / P15-T01).
*/}}
{{- define "docgen.podSecurityContext" -}}
runAsNonRoot: true
seccompProfile:
  type: RuntimeDefault
{{- end }}

{{/*
Container-level securityContext baseline with read-only root FS and dropped caps.
*/}}
{{- define "docgen.containerSecurityContext" -}}
allowPrivilegeEscalation: false
readOnlyRootFilesystem: true
runAsNonRoot: true
capabilities:
  drop:
    - ALL
{{- end }}
