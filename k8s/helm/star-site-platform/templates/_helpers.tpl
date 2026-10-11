{{/* 公共环境注入：与 docker-compose x-app-env 锚点等价 */}}
{{- define "star-site.env" -}}
- name: SPRING_CLOUD_NACOS_SERVER_ADDR
  value: "{{ $.Values.infra.host }}:{{ $.Values.infra.nacosPort }}"
- name: SPRING_DATASOURCE_URL
  value: "jdbc:mysql://{{ $.Values.infra.host }}:{{ $.Values.infra.mysqlPort }}/star_site?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
- name: SPRING_DATASOURCE_USERNAME
  value: "{{ $.Values.global.datasource.username }}"
- name: SPRING_DATASOURCE_PASSWORD
  value: "{{ $.Values.global.datasource.password }}"
- name: JWT_SECRET
  value: "{{ $.Values.global.jwtSecret }}"
- name: TZ
  value: Asia/Shanghai
- name: JAVA_OPTS
  value: "-Xmx256m -Xms64m"
- name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
  value: "{{ $.Values.management.endpoints }}"
{{- end }}
