/**
 * Created by buhe on 16/5/4.
 */
import React, {
    Component,
    PropTypes
} from 'react';
import {
    requireNativeComponent,
    View,
} from 'react-native';

class Player extends Component {

  constructor(props, context) {
    super(props, context);
    this._onLoading = this._onLoading.bind(this);
    this._onPaused = this._onPaused.bind(this);
    this._onShutdown = this._onShutdown.bind(this);
    this._onError = this._onError.bind(this);
    this._onPlaying = this._onPlaying.bind(this);
  }

  _onLoading(event) {
    this.props.onLoading && this.props.onLoading(event.nativeEvent);
  }

  _onPaused(event) {
    this.props.onPaused && this.props.onPaused(event.nativeEvent);
  }

  _onShutdown(event) {
    this.props.onShutdown && this.props.onShutdown(event.nativeEvent);
  }


  _onError(event) {
    this.props.onPlayerError && this.props.onPlayerError(event.nativeEvent);
  }

  _onPlaying(event) {
    this.props.onPlaying && this.props.onPlaying(event.nativeEvent);
  }

  render() {
    const nativeProps = Object.assign({}, this.props);
    Object.assign(nativeProps, {
      onLoading: this._onLoading,
      onPaused: this._onPaused,
      onShutdown: this._onShutdown,
      onPlayerError: this._onPlayerError,
      onPlaying: this._onPlaying,
    });
    return (
        <RCTPlayer
            {...nativeProps}
            />
    )
  }
}

Player.propTypes = {
  source: PropTypes.shape({                          // 是否符合指定格式的物件
    uri: PropTypes.string.isRequired,
    controller: PropTypes.bool, //Android only
    timeout: PropTypes.number, //Android only
    hardCodec: PropTypes.bool, //Android only
    live: PropTypes.bool, //Android only
  }).isRequired,
  started:PropTypes.bool,
  muted:PropTypes.bool, //iOS only
  aspectRatio: PropTypes.oneOf([0, 1, 2, 3, 4]),
  onLoading: PropTypes.func,
  onPaused: PropTypes.func,
  onShutdown: PropTypes.func,
  onPlayerError: PropTypes.func,
  onPlaying: PropTypes.func,
  ...View.propTypes,
}

const RCTPlayer = requireNativeComponent('RCTPlayer', Player);

module.exports = Player;
