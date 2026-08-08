import type { Component } from 'vue'
import {
  FileExcelFilled,
  FileMarkdownFilled,
  FileOutlined,
  FilePdfFilled,
  FilePptFilled,
  FileTextFilled,
  FileWordFilled,
} from '@ant-design/icons-vue'

export interface DocumentIconMeta {
  icon: Component
  color: string
  background: string
  label: string
}

const DEFAULT_ICON: DocumentIconMeta = {
  icon: FileOutlined,
  color: '#64748b',
  background: '#f1f5f9',
  label: 'file',
}

const ICON_MAP: Record<string, DocumentIconMeta> = {
  pdf: { icon: FilePdfFilled, color: '#cf1322', background: '#fff1f0', label: 'PDF' },
  doc: { icon: FileWordFilled, color: '#1677ff', background: '#e6f4ff', label: 'Word' },
  docx: { icon: FileWordFilled, color: '#1677ff', background: '#e6f4ff', label: 'Word' },
  xls: { icon: FileExcelFilled, color: '#389e0d', background: '#f6ffed', label: 'Excel' },
  xlsx: { icon: FileExcelFilled, color: '#389e0d', background: '#f6ffed', label: 'Excel' },
  ppt: { icon: FilePptFilled, color: '#d46b08', background: '#fff7e6', label: 'PPT' },
  pptx: { icon: FilePptFilled, color: '#d46b08', background: '#fff7e6', label: 'PPT' },
  txt: { icon: FileTextFilled, color: '#595959', background: '#f5f5f5', label: 'TXT' },
  md: { icon: FileMarkdownFilled, color: '#531dab', background: '#f9f0ff', label: 'MD' },
  html: { icon: FileTextFilled, color: '#08979c', background: '#e6fffb', label: 'HTML' },
}

function extractExtension(fileName?: string) {
  if (!fileName || !fileName.includes('.')) {
    return ''
  }
  return fileName.slice(fileName.lastIndexOf('.') + 1).toLowerCase()
}

export function getDocumentIconMeta(docType?: string, fileName?: string): DocumentIconMeta {
  const normalizedType = (docType || extractExtension(fileName) || '').toLowerCase()
  return ICON_MAP[normalizedType] || DEFAULT_ICON
}
