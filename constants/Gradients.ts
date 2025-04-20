// 添加预设主题集合
export const GradientPresets = {
  default: {
    countdown: {
      light: { colors: ['#00C9FF', '#92FE9D'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#0077B6', '#48BFE3'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    date: {
      light: { colors: ['#FFF886', '#F072B6'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#E65100', '#EF6C00'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    analytics: {
      light: { colors: ['#3C8CE7', '#00EAFF'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#1B5E20', '#2E7D32'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    }
  },
  blueOcean: {
    countdown: {
      light: { colors: ['#2193b0', '#6dd5ed'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#1A3C40', '#2E5D63'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    date: {
      light: { colors: ['#4CA1AF', '#C4E0E5'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#003B46', '#07575B'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    analytics: {
      light: { colors: ['#5C258D', '#4389A2'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#273746', '#37697A'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    }
  },
  purpleHaze: {
    countdown: {
      light: { colors: ['#8E2DE2', '#4A00E0'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#5B1865', '#301B70'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    date: {
      light: { colors: ['#DA4453', '#89216B'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#5F0F40', '#310E68'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    analytics: {
      light: { colors: ['#FF5F6D', '#FFC371'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#8B0000', '#A65055'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    }
  },
  greenMeadow: {
    countdown: {
      light: { colors: ['#11998e', '#38ef7d'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#155263', '#207561'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    date: {
      light: { colors: ['#56ab2f', '#a8e063'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#2C5530', '#3B7A40'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    },
    analytics: {
      light: { colors: ['#134E5E', '#71B280'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } },
      dark: { colors: ['#0A3628', '#2F5233'], start: { x: 0, y: 0 }, end: { x: 1, y: 1 } }
    }
  }
};

export let CurrentGradients = GradientPresets.default;

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
  countdown: {
    light: {
      colors: ['#00C9FF', '#92FE9D'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    },
    dark: {
      colors: ['#00C9FF', '#92FE9D'],
      start: { x: 0, y: 0 },
      end: { x: 1, y: 1 }
    }
  },
  date: {
    light: {
      colors: ['#FFF886', '#F072B6'],
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
      colors: ['#C9FFBF', '#FFAFBD'],
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
