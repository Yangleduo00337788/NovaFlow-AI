import openaiSvg from '@lobehub/icons-static-svg/icons/openai.svg?raw'
import ollamaSvg from '@lobehub/icons-static-svg/icons/ollama.svg?raw'
import deepseekIcon from '@lobehub/icons-static-svg/icons/deepseek-color.svg?url'
import qwenIcon from '@lobehub/icons-static-svg/icons/qwen-color.svg?url'
import kimiIcon from '@/assets/providers/kimi.svg?url'
import zhipuIcon from '@lobehub/icons-static-svg/icons/zhipu-color.svg?url'
import baichuanIcon from '@lobehub/icons-static-svg/icons/baichuan-color.svg?url'
import minimaxIcon from '@lobehub/icons-static-svg/icons/minimax-color.svg?url'
import doubaoIcon from '@lobehub/icons-static-svg/icons/doubao-color.svg?url'
import baiduIcon from '@lobehub/icons-static-svg/icons/baidu-color.svg?url'
import claudeIcon from '@lobehub/icons-static-svg/icons/claude-color.svg?url'
import geminiIcon from '@lobehub/icons-static-svg/icons/gemini-color.svg?url'
import siliconflowIcon from '@/assets/providers/siliconflow.svg?url'
import { LOGO_ICON_SRC } from '@/constants/brand'

export type ProviderIconDef =
  | { type: 'url'; src: string }
  | { type: 'raw'; svg: string; color?: string }

export const PROVIDER_ICON_MAP: Record<string, ProviderIconDef> = {
  openai: { type: 'raw', svg: openaiSvg, color: '#10a37f' },
  deepseek: { type: 'url', src: deepseekIcon },
  qwen: { type: 'url', src: qwenIcon },
  moonshot: { type: 'url', src: kimiIcon },
  zhipu: { type: 'url', src: zhipuIcon },
  baichuan: { type: 'url', src: baichuanIcon },
  minimax: { type: 'url', src: minimaxIcon },
  doubao: { type: 'url', src: doubaoIcon },
  baidu: { type: 'url', src: baiduIcon },
  siliconflow: { type: 'url', src: siliconflowIcon },
  claude: { type: 'url', src: claudeIcon },
  gemini: { type: 'url', src: geminiIcon },
  ollama: { type: 'raw', svg: ollamaSvg, color: '#000000' },
  custom: { type: 'url', src: LOGO_ICON_SRC },
}

export function getProviderIconDef(code: string): ProviderIconDef {
  return PROVIDER_ICON_MAP[code] ?? PROVIDER_ICON_MAP.custom
}
