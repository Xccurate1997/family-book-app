import { ref } from 'vue'

const STORAGE_KEY = 'bookapp_emoji_size'
const SIZES = {
  small:  { text: '16px', img: 18 },
  medium: { text: '24px', img: 28 },
  large:  { text: '36px', img: 42 }
}

const currentSize = ref(localStorage.getItem(STORAGE_KEY) || 'medium')

export function useEmojiSize() {
  const setSize = (size) => {
    currentSize.value = size
    localStorage.setItem(STORAGE_KEY, size)
  }

  const sizeStyle = () => SIZES[currentSize.value] || SIZES.medium

  return { currentSize, setSize, sizeStyle, SIZES }
}
