module.exports = {
  root: true,
  extends: '@react-native',
  settings: {
    'import/resolver': {
      alias: {
        map: [
          ['@/', './src'],
          // add more aliases here if needed
        ],
        extensions: ['.ts', '.tsx', '.svg'],
      },
    },
  },
};