require "erb"

versions = ["2.21.0", "2.20.1", "2.19.2", "2.18.1", "2.17.2", "2.16.5", "2.15.3", "2.14.5", "2.13.7", "2.12.5", "2.11.4",
            "2.10.5", "2.9.5", "2.8.6", "2.7.6", "2.6.7", "2.5.6", "2.4.9", "2.3.9", "2.2.3", "2.1.4", "2.0.5", "1.9.5"]

task :build_yaml do
  template = File.read('git-tests.gocd.yaml.erb')
  renderer = ERB.new(template, nil, '-')
  File.open('git-tests.gocd.yaml', 'w') do |f|
    f.puts(renderer.result(binding))
  end
end
