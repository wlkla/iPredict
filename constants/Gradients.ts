export const Gradients = {
  primary: {
    colors: ['#00C9FF', '#92FE9D'],
    start: { x: 0, y: 0 },
    end: { x: 1, y: 0 }
  },
  warning: {
    colors: ['#FF5F6D', '#FF9966'],
    start: { x: 0, y: 0 },
    end: { x: 1, y: 0 }
  },
  neutral: {
    colors: ['#808080', '#A9A9A9'],
    start: { x: 0, y: 0 },
    end: { x: 1, y: 0 }
  },
  // 暗色主题的颜色
  primaryDark: {
    colors: ['#0077B6', '#48BFE3'],
    start: { x: 0, y: 0 },
    end: { x: 1, y: 0 }
  },
  warningDark: {
    colors: ['#E63946', '#F4845F'],
    start: { x: 0, y: 0 },
    end: { x: 1, y: 0 }
  },
  // 新增各个页面专用渐变色
  countdown: {
    light: {
      colors: ['#00C9FF', '#92FE9D'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    },
    dark: {
      colors: ['#0077B6', '#48BFE3'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    }
  },
  date: {
    light: {
      colors: ['#FFA726', '#FFCC80'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    },
    dark: {
      colors: ['#E65100', '#EF6C00'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    }
  },
  analytics: {
    light: {
      colors: ['#4CAF50', '#8BC34A'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    },
    dark: {
      colors: ['#1B5E20', '#2E7D32'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    }
  }
};
