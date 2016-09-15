/**
 * Created by buhe on 16/7/14.
 */
const video_encoding = {
  get _240(){
    return 0;
  },
  get _480(){
    return 1;
  },
  get _544(){
    return 2;
  },
  get _720(){
    return 3;
  },
  get _1088(){
    return 4;
  },
}

module.exports = {
  encodingSize:video_encoding
};
