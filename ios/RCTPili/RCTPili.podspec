#
#  Be sure to run `pod spec lint RCTPili.podspec' to ensure this is a
#  valid spec and to remove all comments including this before submitting the spec.
#
#  To learn more about Podspec attributes see http://docs.cocoapods.org/specification.html
#  To see working Podspecs in the CocoaPods repo see https://github.com/CocoaPods/Specs/
#

Pod::Spec.new do |s|
  s.name         = "RCTPili"
  s.version      = "2.0.0"
  s.summary      = "React Native Pili SDK Bridge"

  # This description is used to generate tags and improve search results.
  #   * Think: What does it do? Why did you write it? What is the focus?
  #   * Try to keep it short, snappy and to the point.
  #   * Write the description between the DESC delimiters below.
  #   * Finally, don't worry about the indent, CocoaPods strips it!
  s.description  = <<-DESC
                  React Native Pili SDK Bridge
                   DESC

  s.homepage     = "http://www.airapps.cn/package/react-native-pili"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "buhe" => "bugu1986@126.com" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/buhe/react-native-piliv2.git", :tag => "master" }
  s.source_files  = "RCTPili/**/*.{h,m}"
  s.requires_arc = true

  # s.xcconfig = { "HEADER_SEARCH_PATHS" => "$(SDKROOT)/usr/include/libxml2" }
  s.dependency "React"
  s.dependency "PLMediaStreamingKit"
  s.dependency "PLPlayerKit"

end
